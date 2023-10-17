#include "wallet.h"

#include <cstring>
#include <chrono>

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream.hpp>

#include "common.h"
#include "jni_cache.h"
#include "eraser.h"
#include "fd.h"

#include "string_tools.h"

namespace io = boost::iostreams;

namespace monero {

using namespace std::chrono_literals;
using namespace epee::string_tools;

static_assert(COIN == 1e12, "Monero atomic unit must be 1e-12 XMR");
static_assert(CRYPTONOTE_MAX_BLOCK_NUMBER == 500000000,
              "Min timestamp must be higher than max block height");
static_assert(CRYPTONOTE_DEFAULT_TX_SPENDABLE_AGE == 10, ""); // TODO
static_assert(DIFFICULTY_TARGET_V2 == 120, "");

Wallet::Wallet(
    JNIEnv* env,
    int network_id,
    const JvmRef<jobject>& wallet_native)
    : m_wallet(static_cast<cryptonote::network_type>(network_id),
               0,    /* kdf_rounds */
               true, /* unattended */
               std::make_unique<RemoteNodeClientFactory>(env, wallet_native)),
      m_callback(env, wallet_native),
      m_account_ready(false),
      m_blockchain_height(1),
      m_restore_height(0),
      m_refresh_running(false),
      m_refresh_canceled(false) {
  // Use a bogus ipv6 address as a placeholder for the daemon address.
  LOG_FATAL_IF(!m_wallet.init("[100::/64]", {}, {}, 0, false),
               "Init failed");
  m_wallet.stop();
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

void Wallet::restoreAccount(const std::vector<char>& secret_scalar, uint64_t restore_point) {
  LOG_FATAL_IF(m_account_ready, "Account should not be reinitialized");
  std::lock_guard<std::mutex> lock(m_wallet_mutex);
  auto& account = m_wallet.get_account();
  generateAccountKeys(account, secret_scalar);
  if (restore_point < CRYPTONOTE_MAX_BLOCK_NUMBER) {
    m_restore_height = restore_point;
  } else {
    if (restore_point > account.get_createtime()) {
      account.set_createtime(restore_point);
    }
    m_restore_height = estimateRestoreHeight(account.get_createtime());
  }
  m_wallet.rescan_blockchain(true, false, false);
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
  set_current_blockchain_height(m_wallet.get_blockchain_current_height());
  captureTxHistorySnapshot(m_tx_history);
  m_account_ready = true;
  return true;
}

bool Wallet::writeTo(std::ostream& output) {
  return suspendRefreshAndRunLocked([&]() -> bool {
    binary_archive < true > ar(output);
    if (!serialization::serialize_noeof(ar, *this))
      return false;
    if (!serialization::serialize_noeof(ar, require_account()))
      return false;
    if (!serialization::serialize(ar, m_wallet))
      return false;
    return true;
  });
}

template<typename Consumer>
void Wallet::withTxHistory(Consumer consumer) {
  std::lock_guard<std::mutex> lock(m_tx_history_mutex);
  consumer(m_tx_history);
}

std::string Wallet::public_address() const {
  auto account = const_cast<Wallet*>(this)->require_account();
  return account.get_public_address_str(m_wallet.nettype());
}

void Wallet::set_current_blockchain_height(uint64_t height) {
  LOG_FATAL_IF(height >= CRYPTONOTE_MAX_BLOCK_NUMBER, "Blockchain max height reached");
  m_blockchain_height = height;
}

cryptonote::account_base& Wallet::require_account() {
  LOG_FATAL_IF(!m_account_ready, "Account is not initialized");
  return m_wallet.get_account();
}

const payment_details* find_matching_payment(
    const std::list<std::pair<crypto::hash, payment_details>> pds,
    uint64_t amount,
    const crypto::hash& txid,
    const cryptonote::subaddress_index& subaddr_index) {
  if (txid == crypto::null_hash) {
    return nullptr;
  }
  for (const auto& p: pds) {
    const auto& pd = p.second;
    if (pd.m_amount == amount && pd.m_tx_hash == txid && pd.m_subaddr_index == subaddr_index) {
      return &pd;
    }
  }
  return nullptr;
};

// Only call this function from the callback thread or during initialization,
// as there is no locking mechanism to safeguard reading transaction history
// from wallet2.
void Wallet::captureTxHistorySnapshot(std::vector<TxInfo>& snapshot) {
  snapshot.clear();

  std::vector<transfer_details> tds;
  m_wallet.get_transfers(tds);

  uint64_t min_height = 0;

  std::list<std::pair<crypto::hash, payment_details>> pds;
  std::list<std::pair<crypto::hash, pool_payment_details>> upds;
  std::list<std::pair<crypto::hash, confirmed_transfer_details>> txs;
  std::list<std::pair<crypto::hash, unconfirmed_transfer_details>> utxs;
  m_wallet.get_payments(pds, min_height);
  m_wallet.get_unconfirmed_payments(upds, min_height);
  m_wallet.get_payments_out(txs, min_height);
  m_wallet.get_unconfirmed_payments_out(utxs);

  // Iterate through the known owned outputs (incoming transactions).
  for (const auto& td: tds) {
    snapshot.emplace_back(td.m_txid, TxInfo::INCOMING);
    TxInfo& recv = snapshot.back();
    recv.m_key = td.get_public_key();
    recv.m_key_image = td.m_key_image;
    recv.m_key_image_known = td.m_key_image_known;
    recv.m_subaddress_major = td.m_subaddr_index.major;
    recv.m_subaddress_minor = td.m_subaddr_index.minor;
    recv.m_recipient = m_wallet.get_subaddress_as_str(td.m_subaddr_index);
    recv.m_amount = td.m_amount;
    recv.m_unlock_time = td.m_tx.unlock_time;

    // Check if the payment exists and update metadata if found.
    const auto* pd = find_matching_payment(pds, td.m_amount, td.m_txid, td.m_subaddr_index);
    if (pd) {
      recv.m_height = pd->m_block_height;
      recv.m_timestamp = pd->m_timestamp;
      recv.m_fee = pd->m_fee;
      recv.m_coinbase = pd->m_coinbase;
      recv.m_state = TxInfo::ON_CHAIN;
    } else {
      recv.m_state = TxInfo::OFF_CHAIN;
    }
  }

  // Confirmed outgoing transactions.
  for (const auto& pair: txs) {
    const auto& tx = pair.second;
    uint64_t fee = tx.m_amount_in - tx.m_amount_out;

    for (const auto& dest: tx.m_dests) {
      snapshot.emplace_back(pair.first, TxInfo::OUTGOING);
      TxInfo& spent = snapshot.back();
      spent.m_recipient = dest.address(m_wallet.nettype(), tx.m_payment_id);
      spent.m_amount = dest.amount;
      spent.m_height = tx.m_block_height;
      spent.m_unlock_time = tx.m_unlock_time;
      spent.m_timestamp = tx.m_timestamp;
      spent.m_fee = fee;
      spent.m_change = tx.m_change;
      spent.m_state = TxInfo::ON_CHAIN;
    }

    for (const auto& in: tx.m_tx.vin) {
      if (in.type() != typeid(cryptonote::txin_to_key)) continue;
      const auto& txin = boost::get<cryptonote::txin_to_key>(in);
      snapshot.emplace_back(pair.first, TxInfo::OUTGOING);
      TxInfo& spent = snapshot.back();
      spent.m_key_image = txin.k_image;
      spent.m_key_image_known = true;
      spent.m_amount = txin.amount;
      spent.m_height = tx.m_block_height;
      spent.m_unlock_time = tx.m_unlock_time;
      spent.m_timestamp = tx.m_timestamp;
      spent.m_fee = fee;
      spent.m_state = TxInfo::ON_CHAIN;
    }
  }

  // Unconfirmed outgoing transactions.
  for (const auto& pair: utxs) {
    const auto& utx = pair.second;
    uint64_t fee = utx.m_amount_in - utx.m_amount_out;
    auto state = (utx.m_state == unconfirmed_transfer_details::pending)
                    ? TxInfo::PENDING
                    : TxInfo::FAILED;

    for (const auto& dest: utx.m_dests) {
      if (const auto dest_subaddr_idx = m_wallet.get_subaddress_index(dest.addr)) {
        // Add pending transfers to our own wallet.
        snapshot.emplace_back(pair.first, TxInfo::INCOMING);
        TxInfo& recv = snapshot.back();
        // TODO: recv.m_key
        recv.m_recipient = m_wallet.get_subaddress_as_str(*dest_subaddr_idx);
        recv.m_subaddress_major = (*dest_subaddr_idx).major;
        recv.m_subaddress_minor = (*dest_subaddr_idx).minor;
        recv.m_amount = dest.amount;
        recv.m_unlock_time = utx.m_tx.unlock_time;
        recv.m_timestamp = utx.m_timestamp;
        recv.m_fee = fee;
        recv.m_state = state;
      } else {
        snapshot.emplace_back(pair.first, TxInfo::OUTGOING);
        TxInfo& spent = snapshot.back();
        spent.m_recipient = dest.address(m_wallet.nettype(), utx.m_payment_id);
        spent.m_amount = dest.amount;
        spent.m_unlock_time = utx.m_tx.unlock_time;
        spent.m_timestamp = utx.m_timestamp;
        spent.m_fee = fee;
        spent.m_change = utx.m_change;
        spent.m_state = state;
      }
    }

    // Change is ours too.
    if (utx.m_change > 0) {
      snapshot.emplace_back(pair.first, TxInfo::INCOMING);
      TxInfo& change = snapshot.back();
      // TODO: change.m_key
      change.m_recipient = m_wallet.get_subaddress_as_str({utx.m_subaddr_account, 0});
      change.m_subaddress_major = utx.m_subaddr_account;
      change.m_subaddress_minor = 0;  // All changes go to 0-th subaddress
      change.m_amount = utx.m_change;
      change.m_unlock_time = utx.m_tx.unlock_time;
      change.m_timestamp = utx.m_timestamp;
      change.m_fee = fee;
      change.m_state = state;
    }

    for (const auto& in: utx.m_tx.vin) {
      if (in.type() != typeid(cryptonote::txin_to_key)) continue;
      const auto& txin = boost::get<cryptonote::txin_to_key>(in);
      snapshot.emplace_back(pair.first, TxInfo::OUTGOING);
      TxInfo& spent = snapshot.back();
      spent.m_key_image = txin.k_image;
      spent.m_key_image_known = true;
      spent.m_amount = txin.amount;
      spent.m_timestamp = utx.m_timestamp;
      spent.m_fee = fee;
      spent.m_state = state;
    }
  }

  // Add outputs of unconfirmed payments pending in the pool.
  for (const auto& pair: upds) {
    const auto& upd = pair.second.m_pd;
    bool double_spend_seen = pair.second.m_double_spend_seen; // Unused
    // Denormalize individual amounts sent to a single subaddress in a single tx.
    for (uint64_t amount: upd.m_amounts) {
      snapshot.emplace_back(upd.m_tx_hash, TxInfo::INCOMING);
      TxInfo& recv = snapshot.back();
      // TODO: recv.m_key
      recv.m_recipient = m_wallet.get_subaddress_as_str(upd.m_subaddr_index);
      recv.m_subaddress_major = upd.m_subaddr_index.major;
      recv.m_subaddress_minor = upd.m_subaddr_index.minor;
      recv.m_amount = amount;
      recv.m_height = upd.m_block_height;
      recv.m_unlock_time = upd.m_unlock_time;
      recv.m_timestamp = upd.m_timestamp;
      recv.m_fee = upd.m_fee;
      recv.m_coinbase = upd.m_coinbase;
      recv.m_state = TxInfo::PENDING;
    }
  }
}

void Wallet::handleNewBlock(uint64_t height, bool refresh_running) {
  set_current_blockchain_height(height);
  if (m_balance_changed) {
    m_tx_history_mutex.lock();
    captureTxHistorySnapshot(m_tx_history);
    m_tx_history_mutex.unlock();
  }
  notifyRefresh(!m_balance_changed && refresh_running);
  m_balance_changed = false;
}

void Wallet::handleReorgEvent(uint64_t at_block_height) {
  m_balance_changed = true;
}

void Wallet::handleMoneyEvent(uint64_t at_block_height) {
  m_balance_changed = true;
}

void Wallet::notifyRefresh(bool debounce) {
  static std::chrono::steady_clock::time_point last_time;
  // If debouncing is requested and the blockchain height is a multiple of 100, it limits
  // the notifications to once every 200 ms.
  uint32_t height = current_blockchain_height();
  if (debounce) {
    if (height % 100 == 0) {
      auto now = std::chrono::steady_clock::now();
      if (now - last_time >= 200.ms) {
        last_time = now;
        debounce = false;
      }
    }
  } else {
    last_time = std::chrono::steady_clock::now();
  }
  if (!debounce) {
    m_callback.callVoidMethod(getJniEnv(), WalletNative_onRefresh,
                              height, m_balance_changed);
  }
}

Wallet::Status Wallet::nonReentrantRefresh(bool skip_coinbase) {
  LOG_FATAL_IF(m_refresh_running.exchange(true),
               "Refresh should not be called concurrently");
  Status ret;
  std::unique_lock<std::mutex> wallet_lock(m_wallet_mutex);
  m_wallet.set_refresh_type(skip_coinbase ? tools::wallet2::RefreshType::RefreshNoCoinbase
                                          : tools::wallet2::RefreshType::RefreshDefault);
  while (!m_refresh_canceled) {
    m_wallet.set_refresh_from_block_height(m_restore_height);
    try {
      // refresh() will block until stop() is called or it syncs successfully.
      m_wallet.refresh(false /* trusted_daemon */);
      if (!m_wallet.stopped()) {
        m_wallet.stop();
        ret = Status::OK;
        break;
      }
    } catch (const tools::error::no_connection_to_daemon&) {
      ret = Status::NO_NETWORK_CONNECTIVITY;
      break;
    } catch (const tools::error::refresh_error&) {
      ret = Status::REFRESH_ERROR;
      break;
    }
    m_refresh_cond.wait(wallet_lock);
  }
  if (m_refresh_canceled) {
    m_refresh_canceled = false;
    ret = Status::INTERRUPTED;
  }
  m_refresh_running.store(false);
  // Ensure the latest block and pool state are consistently processed.
  handleNewBlock(m_wallet.get_blockchain_current_height(), false);
  return ret;
}

template<typename T>
auto Wallet::suspendRefreshAndRunLocked(T block) -> decltype(block()) {
  std::unique_lock<std::mutex> wallet_lock(m_wallet_mutex, std::try_to_lock);
  if (!wallet_lock.owns_lock()) {
    JNIEnv* env = getJniEnv();
    for (;;) {
      if (!m_wallet.stopped()) {
        m_wallet.stop();
        m_callback.callVoidMethod(env, WalletNative_onSuspendRefresh, true);
      }
      if (wallet_lock.try_lock()) {
        break;
      }
      std::this_thread::yield();
    }
    m_callback.callVoidMethod(env, WalletNative_onSuspendRefresh, false);
    m_refresh_cond.notify_one();
  }
  // Call the lambda and release the mutex upon completion.
  return block();
}

void Wallet::cancelRefresh() {
  suspendRefreshAndRunLocked([&]() {
    m_refresh_canceled = true;
  });
}

void Wallet::setRefreshSince(long height_or_timestamp) {
  suspendRefreshAndRunLocked([&]() {
    if (height_or_timestamp < CRYPTONOTE_MAX_BLOCK_NUMBER) {
      m_restore_height = height_or_timestamp;
    } else {
      LOG_FATAL("TODO");
    }
  });
}

extern "C"
JNIEXPORT jlong JNICALL
Java_im_molly_monero_WalletNative_nativeCreate(
    JNIEnv* env,
    jobject thiz,
    jint network_id) {
  auto wallet = new Wallet(env, network_id, JvmParamRef<jobject>(thiz));
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
    jlong restore_point) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  std::vector<char> secret_scalar = jvmToNativeByteArray(
      env, JvmParamRef<jbyteArray>(p_secret_scalar));
  Eraser secret_eraser(secret_scalar);
  wallet->restoreAccount(secret_scalar, restore_point);
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
Java_im_molly_monero_WalletNative_nativeNonReentrantRefresh
    (JNIEnv* env,
     jobject thiz,
     jlong handle,
     jboolean skip_coinbase) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return wallet->nonReentrantRefresh(skip_coinbase);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_WalletNative_nativeCancelRefresh(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  wallet->cancelRefresh();
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
Java_im_molly_monero_WalletNative_nativeGetAccountPrimaryAddress(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return nativeToJvmString(env, wallet->public_address()).Release();
}

extern "C"
JNIEXPORT jint JNICALL
Java_im_molly_monero_WalletNative_nativeGetCurrentBlockchainHeight(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return wallet->current_blockchain_height();
}

ScopedJvmLocalRef<jobject> nativeToJvmTxInfo(JNIEnv* env,
                                             const TxInfo& info) {
  LOG_FATAL_IF(info.m_height >= CRYPTONOTE_MAX_BLOCK_NUMBER, "Blockchain max height reached");
  // TODO: Check amount overflow
  return {env, TxInfoClass.newObject(
      env, TxInfo_ctor,
      nativeToJvmString(env, pod_to_hex(info.m_tx_hash)).obj(),
      nativeToJvmString(env, pod_to_hex(info.m_key)).obj(),
      info.m_key_image_known ? nativeToJvmString(env, pod_to_hex(info.m_key_image)).obj(): nullptr,
      info.m_subaddress_major,
      info.m_subaddress_minor,
      (!info.m_recipient.empty()) ? nativeToJvmString(env, info.m_recipient).obj() : nullptr,
      info.m_amount,
      static_cast<jint>(info.m_height),
      info.m_state,
      info.m_unlock_time,
      info.m_timestamp,
      info.m_fee,
      info.m_coinbase,
      info.m_type == TxInfo::INCOMING)
  };
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_im_molly_monero_WalletNative_nativeGetTxHistory(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  ScopedJvmLocalRef<jobjectArray> j_array;
  wallet->withTxHistory([env, &j_array](std::vector<TxInfo> const& txs) {
    j_array = nativeToJvmObjectArray(env,
                                     txs,
                                     TxInfoClass.getClass(),
                                     &nativeToJvmTxInfo);
  });
  return j_array.Release();
}

}  // namespace monero
