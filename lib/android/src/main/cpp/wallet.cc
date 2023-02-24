#include "wallet.h"

#include <cstring>
#include <chrono>

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream.hpp>

#include "common.h"
#include "jni_cache.h"
#include "eraser.h"
#include "fd.h"

namespace io = boost::iostreams;

namespace monero {

using namespace std::chrono_literals;

static_assert(COIN == 1e12, "Monero atomic unit must be 1e-12 XMR");
static_assert(CRYPTONOTE_MAX_BLOCK_NUMBER == 500000000,
              "Min timestamp must be higher than max block height");

Wallet::Wallet(
    JNIEnv* env,
    int network_id,
    std::unique_ptr<HttpClientFactory> http_client_factory,
    const JvmRef<jobject>& callback)
    : m_wallet(static_cast<cryptonote::network_type>(network_id),
               0,    /* kdf_rounds */
               true, /* unattended */
               std::move(http_client_factory)),
      m_callback(env, callback),
      m_account_ready(false),
      m_blockchain_height(1),
      m_restore_height(0),
      m_refresh_continue(false) {
  // Use a bogus ipv6 address as a placeholder for the daemon address.
  LOG_FATAL_IF(!m_wallet.init("[100::/64]", {}, {}, 0, false),
               "Init failed");
  m_wallet.callback(this);
}

// Generate keypairs deterministically.  Account creation time will be set
// to Monero epoch.
void generateAccountKeys(cryptonote::account_base& account,
                         const std::vector<char>& secret_scalar) {
  crypto::secret_key secret_key;
  LOG_FATAL_IF(secret_scalar.size() != sizeof(secret_key.data),
               "Secret key size mismatch");
  std::copy(secret_scalar.begin(), secret_scalar.end(), secret_key.data);
  crypto::secret_key gen = account.generate(secret_key, true, false);
  LOG_FATAL_IF(gen != secret_key);
}

void Wallet::restoreAccount(const std::vector<char>& secret_scalar, uint64_t account_timestamp) {
  LOG_FATAL_IF(m_account_ready, "Account should not be reinitialized");
  std::lock_guard<std::mutex> lock(m_wallet_mutex);
  auto& account = m_wallet.get_account();
  generateAccountKeys(account, secret_scalar);
  if (account_timestamp > account.get_createtime()) {
    account.set_createtime(account_timestamp);
  }
  m_wallet.rescan_blockchain(true, false, false);
  m_restore_height = estimateRestoreHeight(account.get_createtime());
  m_account_ready = true;
}

uint64_t Wallet::estimateRestoreHeight(uint64_t timestamp) {
  // Apply -1 month adjustment for fluctuations in block time, just like
  // estimate_blockchain_height() does when node's height is unavailable.
  const int secs_per_month = 60 * 60 * 24 * 30;
  LOG_FATAL_IF(secs_per_month > timestamp);
  return m_wallet.get_approximate_blockchain_height(timestamp - secs_per_month);
}

bool Wallet::parseFrom(std::istream& input) {
  LOG_FATAL_IF(m_account_ready, "Account should not be reinitialized");
  std::ostringstream ss;
  ss << input.rdbuf();
  const std::string buf = ss.str();
  binary_archive<false> ar{epee::strspan<std::uint8_t>(buf)};
  std::lock_guard<std::mutex> lock(m_wallet_mutex);
  if (!serialization::serialize_noeof(ar, *this))
    return false;
  if (!serialization::serialize_noeof(ar, m_wallet.get_account()))
    return false;
  if (!serialization::serialize(ar, m_wallet))
    return false;
  m_wallet.get_transfers(m_tx_outs);
  m_account_ready = true;
  return true;
}

bool Wallet::writeTo(std::ostream& output) {
  return pauseRefreshAndRunLocked([&]() -> bool {
    binary_archive<true> ar(output);
    if (!serialization::serialize_noeof(ar, *this))
      return false;
    if (!serialization::serialize_noeof(ar, require_account()))
      return false;
    if (!serialization::serialize(ar, m_wallet))
      return false;
    return true;
  });
}

template<typename Callback>
void Wallet::getOwnedTxOuts(Callback callback) {
  std::lock_guard<std::mutex> lock(m_tx_outs_mutex);
  callback(m_tx_outs);
}

std::string Wallet::public_address() const {
  auto account = const_cast<Wallet*>(this)->require_account();
  return account.get_public_address_str(m_wallet.nettype());
}

cryptonote::account_base& Wallet::require_account() {
  LOG_FATAL_IF(!m_account_ready, "Account is not initialized");
  return m_wallet.get_account();
}

// Reading m_transfers from wallet2 is not guarded by any lock; call this function only
// from wallet2's callback thread.
void Wallet::handleBalanceChanged(uint64_t at_block_height) {
  LOGV("handleBalanceChanged(%lu)", at_block_height);
  m_tx_outs_mutex.lock();
  m_wallet.get_transfers(m_tx_outs);
  m_tx_outs_mutex.unlock();
  m_blockchain_height = at_block_height;
  JNIEnv* env = getJniEnv();
  m_callback.callVoidMethod(env, Wallet_onRefresh, at_block_height, true);
  LOG_FATAL_IF(checkException(env));
}

void Wallet::handleNewBlock(uint64_t height) {
  m_blockchain_height = height;
  // Notify blockchain height every one second.
  static std::chrono::steady_clock::time_point last_time;
  auto now = std::chrono::steady_clock::now();
  if (now - last_time >= 1.s) {
    last_time = now;
    m_callback.callVoidMethod(getJniEnv(), Wallet_onRefresh, height, false);
  }
}

Wallet::Status Wallet::refreshLoopUntilSynced(bool skip_coinbase) {
  std::unique_lock<std::mutex> lock(m_wallet_mutex);
  m_refresh_continue = true;
  Status ret = Status::INTERRUPTED;
  while (m_refresh_continue) {
    try {
      m_wallet.set_refresh_type(skip_coinbase ? tools::wallet2::RefreshType::RefreshNoCoinbase
                                              : tools::wallet2::RefreshType::RefreshDefault);
      m_wallet.set_refresh_from_block_height(m_restore_height);
      // It will block until we call stop() or it sync successfully.
      m_wallet.refresh(false);
      if (!m_wallet.stopped()) {
        ret = Status::OK;
        break;
      }
    } catch (const tools::error::no_connection_to_daemon&) {
      ret = Status::NO_NETWORK_CONNECTIVITY;
      break;
    } catch (const tools::error::refresh_error) {
      ret = Status::REFRESH_ERROR;
      break;
    }
    m_refresh_cond.wait(lock);
  }
  lock.unlock();
  // Always notify the last block height.
  m_callback.callVoidMethod(getJniEnv(), Wallet_onRefresh, m_blockchain_height, false);
  return ret;
}

template<typename T>
auto Wallet::pauseRefreshAndRunLocked(T block) -> decltype(block()) {
  std::unique_lock<std::mutex> lock(m_wallet_mutex, std::defer_lock);
  while (!lock.try_lock()) {
    m_wallet.stop();
    std::this_thread::yield();
  }
  auto res = block();
  m_refresh_cond.notify_one();
  return res;
}

void Wallet::stopRefresh() {
  pauseRefreshAndRunLocked([&]() -> int {
    m_refresh_continue = false;
    return 0;
  });
}

void Wallet::setRefreshSince(long height_or_timestamp) {
  pauseRefreshAndRunLocked([&]() -> int {
    if (height_or_timestamp < CRYPTONOTE_MAX_BLOCK_NUMBER) {
      m_restore_height = height_or_timestamp;
    } else {
      LOG_FATAL("TODO");
    }
    return 0;
  });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_im_molly_monero_WalletNative_nativeCreate(
    JNIEnv* env,
    jobject thiz,
    jint network_id,
    jobject p_remote_node_client) {
  auto wallet = new Wallet(
      env, network_id,
      std::make_unique<RemoteNodeClientFactory>(
          env, JvmParamRef<jobject>(p_remote_node_client)),
      JvmParamRef<jobject>(thiz));
  return nativeToJvmPointer(wallet);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_WalletNative_nativeDispose(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  free(wallet);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_WalletNative_nativeRestoreAccount(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jbyteArray p_secret_scalar,
    jlong account_timestamp) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  std::vector<char> secret_scalar = jvmToNativeByteArray(
      env, JvmParamRef<jbyteArray>(p_secret_scalar));
  Eraser secret_eraser(secret_scalar);
  wallet->restoreAccount(secret_scalar, account_timestamp);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_im_molly_monero_WalletNative_nativeLoad(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint fd) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  io::stream<io::file_descriptor_source> in_stream(fd, io::never_close_handle);
  return wallet->parseFrom(in_stream);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_im_molly_monero_WalletNative_nativeSave(
    JNIEnv* env,
    jobject thiz,
    jlong handle, jint fd) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  io::stream<io::file_descriptor_sink> out_stream(fd, io::never_close_handle);
  return wallet->writeTo(out_stream);
}

extern "C"
JNIEXPORT jint JNICALL
Java_im_molly_monero_WalletNative_nativeRefreshLoopUntilSynced
    (JNIEnv* env,
     jobject thiz,
     jlong handle,
     jboolean skip_coinbase) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return wallet->refreshLoopUntilSynced(skip_coinbase);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_WalletNative_nativeStopRefresh(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  wallet->stopRefresh();
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_im_molly_monero_WalletNative_nativeRefreshIsRunning(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return wallet->refresh_is_running();
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_WalletNative_nativeSetRefreshSince(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jlong height_or_timestamp) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  wallet->setRefreshSince(height_or_timestamp);
}

//extern "C"
//JNIEXPORT jbyteArray JNICALL
//Java_im_molly_monero_Wallet_nativeGetViewPublicKey(
//    JNIEnv* env,
//    jobject thiz,
//    jlong handle) {
//  auto* wallet = reinterpret_cast<Wallet*>(handle);
//  auto* key = &wallet->keys().m_account_address.m_view_public_key;
//  return nativeToJvmByteArray(env, key->data, sizeof(key->data)).Release();
//}
//
//extern "C"
//JNIEXPORT jbyteArray JNICALL
//Java_im_molly_monero_Wallet_nativeGetSpendPublicKey(
//    JNIEnv* env,
//    jobject thiz,
//    jlong handle) {
//  auto* wallet = reinterpret_cast<Wallet*>(handle);
//  auto* key = &wallet->keys().m_account_address.m_spend_public_key;
//  return nativeToJvmByteArray(env, key->data, sizeof(key->data)).Release();
//}

extern "C"
JNIEXPORT jstring JNICALL
Java_im_molly_monero_WalletNative_nativeGetPrimaryAccountAddress(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return nativeToJvmString(env, wallet->public_address()).Release();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_im_molly_monero_WalletNative_nativeGetCurrentBlockchainHeight(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  uint64_t height = wallet->current_blockchain_height();
  LOG_FATAL_IF(height > std::numeric_limits<jlong>::max(),
               "Blockchain height overflowed jlong");
  return static_cast<jlong>(height);
}

ScopedJvmLocalRef<jobject> nativeToJvmOwnedTxOut(JNIEnv* env,
                                                 const TxOut& tx_out) {
  LOG_FATAL_IF(tx_out.m_spent
               && (tx_out.m_spent_height == 0 ||
                   tx_out.m_spent_height < tx_out.m_block_height),
               "Unexpected spent block height in tx output");
  return {env, OwnedTxOut.newObject(
      env,
      OwnedTxOut_ctor,
      nativeToJvmByteArray(env, tx_out.m_txid.data, sizeof(tx_out.m_txid.data)).obj(),
      tx_out.m_amount,
      tx_out.m_block_height,
      tx_out.m_spent_height)
  };
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_im_molly_monero_WalletNative_nativeGetOwnedTxOuts(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  ScopedJvmLocalRef<jobjectArray> j_array;
  wallet->getOwnedTxOuts([env, &j_array](std::vector<TxOut> const& tx_outs) {
    j_array = nativeToJvmObjectArray(env,
                                     tx_outs,
                                     OwnedTxOut.getClass(),
                                     &nativeToJvmOwnedTxOut);
  });
  return j_array.Release();
}

}  // namespace monero
