#include "wallet.h"

#include <cstring>
#include <chrono>

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream.hpp>

#include "common/debug.h"
#include "common/eraser.h"

#include "jni_cache.h"
#include "fd.h"

#include "string_tools.h"

namespace io = boost::iostreams;

namespace monero {

using namespace std::chrono_literals;
using namespace epee::string_tools;

// Ensure constant values match the expected values in Kotlin.
static_assert(COIN == 1e12,
              "Monero atomic unit must be 1e-12 XMR");
static_assert(CRYPTONOTE_MAX_BLOCK_NUMBER == 500000000,
              "Min timestamp must be higher than max block height");
static_assert(CRYPTONOTE_DEFAULT_TX_SPENDABLE_AGE == 10,
              "CRYPTONOTE_DEFAULT_TX_SPENDABLE_AGE mismatch");
static_assert(DIFFICULTY_TARGET_V2 == 120,
              "DIFFICULTY_TARGET_V2 mismatch");
static_assert(PER_KB_FEE_QUANTIZATION_DECIMALS == 8,
              "PER_KB_FEE_QUANTIZATION_DECIMALS mismatch");

Wallet::Wallet(
    JNIEnv* env,
    int network_id,
    const JavaRef<jobject>& wallet_native)
    : m_wallet(static_cast<cryptonote::network_type>(network_id),
               0,    /* kdf_rounds */
               true, /* unattended */
               std::make_unique<RemoteNodeClientFactory>(env, wallet_native)),
      m_callback(env, wallet_native),
      m_account_ready(false),
      m_last_block_height(1),
      m_last_block_timestamp(0),
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
void GenerateAccountKeys(cryptonote::account_base& account,
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
  GenerateAccountKeys(account, secret_scalar);
  m_subaddresses[{0, 0}] = m_wallet.get_subaddress_as_str({0, 0});
  if (restore_point < CRYPTONOTE_MAX_BLOCK_NUMBER) {
    m_restore_height = restore_point;
    m_last_block_timestamp = 0;
  } else {
    if (restore_point > account.get_createtime()) {
      account.set_createtime(restore_point);
    }
    m_restore_height = estimateRestoreHeight(account.get_createtime());
    m_last_block_timestamp = account.get_createtime();
  }
  m_last_block_height = (m_restore_height == 0) ? 1 : m_restore_height;
  LOGD("Restoring account: restore_point=%" PRIu64 ", computed restore_height=%" PRIu64,
       restore_point, m_restore_height);
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
  updateSubaddressMap(m_subaddresses);
  captureTxHistorySnapshot(m_tx_history);
  m_account_ready = true;
  return true;
}

bool Wallet::writeTo(std::ostream& output) {
  return suspendRefreshAndRunLocked([&]() -> bool {
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

std::string FormatAccountAddress(
    const std::pair<cryptonote::subaddress_index, std::string>& pair) {
  std::stringstream ss;
  ss << pair.first.major << "/" << pair.first.minor << "/" << pair.second;
  return ss.str();
}

std::string Wallet::addDetachedSubAddress(uint32_t index_major, uint32_t index_minor) {
  return suspendRefreshAndRunLocked([&]() {
    cryptonote::subaddress_index index = {index_major, index_minor};
    m_wallet.create_one_off_subaddress(index);
    return addSubaddressInternal(index);
  });
}

std::string Wallet::createSubAddressAccount() {
  return suspendRefreshAndRunLocked([&]() {
    uint32_t index_major = m_wallet.get_num_subaddress_accounts();
    m_wallet.add_subaddress_account("");
    return addSubaddressInternal({index_major, 0});
  });
}

std::string Wallet::createSubAddress(uint32_t index_major) {
  return suspendRefreshAndRunLocked([&]() {
    uint32_t index_minor = m_wallet.get_num_subaddresses(index_major);
    m_wallet.add_subaddress(index_major, "");
    return addSubaddressInternal({index_major, index_minor});
  });
}

std::string Wallet::addSubaddressInternal(const cryptonote::subaddress_index& index) {
  std::string subaddress = m_wallet.get_subaddress_as_str(index);
  std::unique_lock<std::mutex> lock(m_subaddresses_mutex);
  auto ret = m_subaddresses.insert({index, subaddress});
  return FormatAccountAddress(*ret.first);
}

std::unique_ptr<PendingTransfer> Wallet::createPayment(
    const std::vector<std::string>& addresses,
    const std::vector<uint64_t>& amounts,
    int priority,
    uint32_t account_index,
    const std::set<uint32_t>& subaddr_indexes) {
  std::unique_lock<std::mutex> wallet_lock(m_wallet_mutex);

  std::vector<cryptonote::tx_destination_entry> dsts;
  dsts.reserve(addresses.size());

  for (size_t i = 0; i < addresses.size(); ++i) {
    const std::string& address = addresses[i];
    cryptonote::address_parse_info info;
    if (!cryptonote::get_account_address_from_str(info, m_wallet.nettype(), address)) {
      LOG_FATAL("Failed to parse recipient address: %s", address.c_str());
    }
    LOG_FATAL_IF(info.has_payment_id);
    cryptonote::tx_destination_entry de;
    de.original = address;
    de.addr = info.address;
    de.amount = amounts.at(i);
    de.is_subaddress = info.is_subaddress;
    de.is_integrated = false;
    dsts.push_back(de);
  }

  auto ptxs = m_wallet.create_transactions_2(
      dsts,
      m_wallet.get_min_ring_size() - 1,
      priority,
      {}, /* extra */
      account_index,
      subaddr_indexes);

  return std::make_unique<PendingTransfer>(ptxs);
}

void Wallet::commit_transfer(PendingTransfer& pending_transfer) {
  std::unique_lock<std::mutex> wallet_lock(m_wallet_mutex);

  while (!pending_transfer.m_ptxs.empty()) {
    m_wallet.commit_tx(pending_transfer.m_ptxs.back());
    m_balance_changed = true;
    pending_transfer.m_ptxs.pop_back();
  }

  if (m_balance_changed) {
    processBalanceChanges(false);
  }
}

template<typename Consumer>
void Wallet::withTxHistory(Consumer consumer) {
  std::lock_guard<std::mutex> lock(m_tx_history_mutex);
  consumer(m_tx_history);
}

std::vector<uint64_t> Wallet::fetchBaseFeeEstimate() {
  return m_wallet.get_dynamic_base_fee_scaling_estimate();
}

std::string Wallet::public_address() const {
  return require_account().get_public_address_str(m_wallet.nettype());
}

std::vector<std::string> Wallet::formatted_subaddresses(uint32_t index_major) {
  std::lock_guard<std::mutex> lock(m_subaddresses_mutex);

  std::vector<std::string> ret;
  ret.reserve(m_subaddresses.size());

  for (const auto& entry: m_subaddresses) {
    if (index_major == -1 || index_major == entry.first.major) {
      ret.push_back(FormatAccountAddress(entry));
    }
  }

  return ret;
}

cryptonote::account_base& Wallet::require_account() {
  LOG_FATAL_IF(!m_account_ready, "Account is not initialized");
  return m_wallet.get_account();
}

const cryptonote::account_base& Wallet::require_account() const {
  return const_cast<Wallet*>(this)->require_account();
}

crypto::secret_key Wallet::spend_secret_key() const {
  return require_account().get_keys().m_spend_secret_key;
}

crypto::secret_key Wallet::view_secret_key() const {
  return require_account().get_keys().m_view_secret_key;
}

const wallet2::payment_details* Find_payment_by_txid(
    const std::list<std::pair<crypto::hash, wallet2::payment_details>>& pds,
    const crypto::hash& txid) {
  if (txid == crypto::null_hash) {
    return nullptr;
  }
  for (auto it = pds.begin(); it != pds.end(); ++it) {
    const auto pd = &it->second;
    if (txid == pd->m_tx_hash) {
      return pd;
    }
  }
  return nullptr;
}

const wallet2::confirmed_transfer_details* Find_transfer_by_txid(
    const std::list<std::pair<crypto::hash, wallet2::confirmed_transfer_details>>& txs,
    const crypto::hash& txid) {
  if (txid == crypto::null_hash) {
    return nullptr;
  }
  for (auto it = txs.begin(); it != txs.end(); ++it) {
    const auto tx = &it->second;
    if (txid == it->first) {
      return tx;
    }
  }
  return nullptr;
}

// Only call this function from the callback thread or during initialization,
// as there is no locking mechanism to safeguard reading transaction history
// from wallet2.
void Wallet::captureTxHistorySnapshot(std::vector<TxInfo>& snapshot) {
  snapshot.clear();

  std::vector<wallet2::transfer_details> tds;
  m_wallet.get_transfers(tds);

  uint64_t min_height = 0;

  std::list<std::pair<crypto::hash, wallet2::payment_details>> pds;
  std::list<std::pair<crypto::hash, wallet2::pool_payment_details>> upds;
  std::list<std::pair<crypto::hash, wallet2::confirmed_transfer_details>> txs;
  std::list<std::pair<crypto::hash, wallet2::unconfirmed_transfer_details>> utxs;
  m_wallet.get_payments(pds, min_height);
  m_wallet.get_unconfirmed_payments(upds, min_height);
  m_wallet.get_payments_out(txs, min_height);
  m_wallet.get_unconfirmed_payments_out(utxs);

  // Iterate through the known owned outputs.
  for (const auto& td: tds) {
    snapshot.emplace_back(td.m_txid, TxInfo::INCOMING);
    TxInfo& recv = snapshot.back();
    recv.m_public_key = td.get_public_key();
    recv.m_public_key_known = true;
    recv.m_key_image = td.m_key_image;
    recv.m_key_image_known = td.m_key_image_known;
    recv.m_subaddress_major = td.m_subaddr_index.major;
    recv.m_subaddress_minor = td.m_subaddr_index.minor;
    recv.m_amount = td.m_amount;
    recv.m_unlock_time = td.m_tx.unlock_time;

    // Check if the payment or transfer exists and update metadata if found.
    if (const auto* pd = Find_payment_by_txid(pds, td.m_txid)) {
      recv.m_height = pd->m_block_height;
      recv.m_timestamp = pd->m_timestamp;
      recv.m_fee = pd->m_fee;
      recv.m_coinbase = pd->m_coinbase;
      recv.m_state = TxInfo::ON_CHAIN;
    } else if (const auto* tx = Find_transfer_by_txid(txs, td.m_txid)) {
      recv.m_height = tx->m_block_height;
      recv.m_timestamp = tx->m_timestamp;
      recv.m_fee = tx->m_amount_in - tx->m_amount_out;
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

    for (const auto& ring: tx.m_rings) {
      snapshot.emplace_back(pair.first, TxInfo::OUTGOING);
      TxInfo& spent = snapshot.back();
      spent.m_key_image = ring.first;
      spent.m_key_image_known = true;
      spent.m_height = tx.m_block_height;
      spent.m_unlock_time = tx.m_unlock_time;
      spent.m_timestamp = tx.m_timestamp;
      spent.m_fee = fee;
      spent.m_change = tx.m_change;
      spent.m_state = TxInfo::ON_CHAIN;
    }
  }

  // Unconfirmed outgoing transactions.
  for (const auto& pair: utxs) {
    const auto& utx = pair.second;
    uint64_t fee = utx.m_amount_in - utx.m_amount_out;
    auto state = (utx.m_state != wallet2::unconfirmed_transfer_details::failed)
                 ? TxInfo::PENDING
                 : TxInfo::FAILED;

    for (const auto& dest: utx.m_dests) {
      if (const auto dest_subaddr_idx = m_wallet.get_subaddress_index(dest.addr)) {
        // Add pending transfers to our own wallet.
        snapshot.emplace_back(pair.first, TxInfo::INCOMING);
        TxInfo& recv = snapshot.back();
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

    // Change is ours too, but the output is not yet in transfer_details
    if (utx.m_change > 0) {
      snapshot.emplace_back(pair.first, TxInfo::INCOMING);
      TxInfo& change = snapshot.back();
      change.m_subaddress_major = utx.m_subaddr_account;
      change.m_subaddress_minor = 0;  // All changes go to 0-th subaddress
      change.m_amount = utx.m_change;
      change.m_unlock_time = utx.m_tx.unlock_time;
      change.m_timestamp = utx.m_timestamp;
      change.m_fee = fee;
      change.m_state = state;
    }

    for (const auto& ring: utx.m_rings) {
      snapshot.emplace_back(pair.first, TxInfo::OUTGOING);
      TxInfo& spent = snapshot.back();
      spent.m_key_image = ring.first;
      spent.m_key_image_known = true;
      spent.m_timestamp = utx.m_timestamp;
      spent.m_fee = fee;
      spent.m_change = utx.m_change;
      spent.m_state = state;
    }
  }

  // Add outputs of unconfirmed payments pending in the pool.
  for (const auto& pair: upds) {
    const auto& upd = pair.second.m_pd;
    bool double_spend_seen = pair.second.m_double_spend_seen; // Unused
    // Skip unconfirmed transfers sent to our own wallet.
    auto it = std::find_if(
        utxs.begin(), utxs.end(),
        [&](const std::pair<crypto::hash, wallet2::unconfirmed_transfer_details>& utx) {
          return utx.first == upd.m_tx_hash;
        });
    if (it != utxs.end()) continue;
    // Denormalize individual amounts sent to a single subaddress in a single tx.
    for (uint64_t amount: upd.m_amounts) {
      snapshot.emplace_back(upd.m_tx_hash, TxInfo::INCOMING);
      TxInfo& recv = snapshot.back();
      recv.m_subaddress_major = upd.m_subaddr_index.major;
      recv.m_subaddress_minor = upd.m_subaddr_index.minor;
      recv.m_amount = amount;
      recv.m_unlock_time = upd.m_unlock_time;
      recv.m_timestamp = upd.m_timestamp;
      recv.m_fee = upd.m_fee;
      recv.m_coinbase = upd.m_coinbase;
      recv.m_state = TxInfo::PENDING;
    }
  }
}

// Only call this function from the callback thread or during initialization.
void Wallet::updateSubaddressMap(std::map<cryptonote::subaddress_index, std::string>& map) {
  uint32_t num_accounts = m_wallet.get_num_subaddress_accounts();

  for (uint32_t index_major = 0; index_major < num_accounts; ++index_major) {
    uint32_t num_subaddresses = m_wallet.get_num_subaddresses(index_major);

    for (uint32_t index_minor = 0; index_minor < num_subaddresses; ++index_minor) {
      cryptonote::subaddress_index index = {index_major, index_minor};

      if (map.find(index) == map.end()) {
        map[index] = m_wallet.get_subaddress_as_str(index);
      }
    }
  }
}

void Wallet::handleNewBlock(uint64_t height, uint64_t timestamp) {
  LOG_FATAL_IF(height >= CRYPTONOTE_MAX_BLOCK_NUMBER, "Blockchain max height reached");
  m_last_block_height = height;
  m_last_block_timestamp = timestamp;
  processBalanceChanges(true);
}

void Wallet::handleReorgEvent(uint64_t at_block_height) {
  m_balance_changed = true;
}

void Wallet::handleMoneyEvent(uint64_t at_block_height) {
  m_balance_changed = true;
}

void Wallet::processBalanceChanges(bool refresh_running) {
  if (m_balance_changed) {
    m_subaddresses_mutex.lock();
    updateSubaddressMap(m_subaddresses);
    m_subaddresses_mutex.unlock();
    m_tx_history_mutex.lock();
    captureTxHistorySnapshot(m_tx_history);
    m_tx_history_mutex.unlock();
  }
  notifyRefreshState(!m_balance_changed && refresh_running);
  m_balance_changed = false;
}

void Wallet::notifyRefreshState(bool debounce) {
  static std::chrono::steady_clock::time_point last_time;
  // If debouncing is requested and the blockchain height is a multiple of 100, it limits
  // the notifications to once every 200 ms.
  uint32_t height = current_blockchain_height();
  uint64_t ts = current_blockchain_timestamp();
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
    CallVoidMethod(GetJniEnv(), m_callback.obj(), NativeWallet_onRefresh,
                   height, ts, m_balance_changed);
  }
}

Wallet::Status Wallet::nonReentrantRefresh(bool skip_coinbase) {
  LOG_FATAL_IF(m_refresh_running.exchange(true),
               "Refresh should not be called concurrently");
  Status ret;
  std::unique_lock<std::mutex> wallet_lock(m_wallet_mutex);
  m_wallet.set_refresh_type(skip_coinbase ? wallet2::RefreshType::RefreshNoCoinbase
                                          : wallet2::RefreshType::RefreshDefault);
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
    } catch (const error::no_connection_to_daemon&) {
      ret = Status::NO_NETWORK_CONNECTIVITY;
      break;
    } catch (const error::refresh_error&) {
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
  processBalanceChanges(false);
  return ret;
}

template<typename T>
auto Wallet::suspendRefreshAndRunLocked(T block) -> decltype(block()) {
  std::unique_lock<std::mutex> wallet_lock(m_wallet_mutex, std::try_to_lock);
  if (!wallet_lock.owns_lock()) {
    JNIEnv* env = GetJniEnv();
    for (;;) {
      if (!m_wallet.stopped()) {
        m_wallet.stop();
        CallVoidMethod(env, m_callback.obj(),
                       NativeWallet_onSuspendRefresh, true);
      }
      if (wallet_lock.try_lock()) {
        break;
      }
      std::this_thread::yield();
    }
    CallVoidMethod(env, m_callback.obj(),
                   NativeWallet_onSuspendRefresh, false);
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
Java_im_molly_monero_sdk_internal_NativeWallet_nativeCreate(
    JNIEnv* env,
    jobject thiz,
    jint network_id) {
  auto* wallet = new Wallet(env, network_id, JavaParamRef<jobject>(thiz));
  return NativeToJavaPointer(wallet);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeDispose(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  delete reinterpret_cast<Wallet*>(handle);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeRestoreAccount(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jbyteArray j_secret_scalar,
    jlong restore_point) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  std::vector<char> secret_scalar = JavaToNativeByteArray(env, j_secret_scalar);
  Eraser secret_eraser(secret_scalar);
  wallet->restoreAccount(secret_scalar, restore_point);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeLoad(
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
Java_im_molly_monero_sdk_internal_NativeWallet_nativeSave(
    JNIEnv* env,
    jobject thiz,
    jlong handle, jint fd) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  io::stream<io::file_descriptor_sink> out_stream(fd, io::never_close_handle);
  return wallet->writeTo(out_stream);
}

extern "C"
JNIEXPORT jint JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeNonReentrantRefresh
    (JNIEnv* env,
     jobject thiz,
     jlong handle,
     jboolean skip_coinbase) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return wallet->nonReentrantRefresh(skip_coinbase);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeCancelRefresh(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  wallet->cancelRefresh();
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeSetRefreshSince(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jlong height_or_timestamp) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  wallet->setRefreshSince(height_or_timestamp);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeGetSpendSecretKey(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  auto key = wallet->spend_secret_key();
  return NativeToJavaByteArray(env, key.data, sizeof(key.data));
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeGetViewSecretKey(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  auto key = wallet->view_secret_key();
  return NativeToJavaByteArray(env, key.data, sizeof(key.data));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeGetPublicAddress(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return NativeToJavaString(env, wallet->public_address());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeAddDetachedSubAddress(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint sub_address_major,
    jint sub_address_minor) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return NativeToJavaString(
      env, wallet->addDetachedSubAddress(sub_address_major, sub_address_minor));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeCreateSubAddressAccount(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return NativeToJavaString(env, wallet->createSubAddressAccount());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeCreateSubAddress(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint sub_address_major) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  try {
    return NativeToJavaString(env, wallet->createSubAddress(sub_address_major));
  } catch (error::account_index_outofbound& e) {
    return nullptr;
  }
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeGetSubAddresses(
    JNIEnv* env,
    jobject thiz,
    jint sub_address_major,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  try {
    auto subaddresses = wallet->formatted_subaddresses(sub_address_major);
    return NativeToJavaStringArray(env, subaddresses);
  } catch (error::account_index_outofbound& e) {
    return NativeToJavaStringArray(env, {});
  }
}

extern "C"
JNIEXPORT jint JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeGetCurrentBlockchainHeight(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return wallet->current_blockchain_height();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeGetCurrentBlockchainTimestamp(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  return wallet->current_blockchain_timestamp();
}

ScopedJavaLocalRef<jobject> NativeToJavaTxInfo(JNIEnv* env,
                                               const TxInfo& tx) {
  LOG_FATAL_IF(tx.m_height >= CRYPTONOTE_MAX_BLOCK_NUMBER,
               "Blockchain max height reached");
  // TODO: Check amount overflow
  return {env, NewObject(
      env,
      TxInfoClass.obj(), TxInfo_ctor,
      ScopedJavaLocalRef<jstring>(
          env, NativeToJavaString(env, pod_to_hex(tx.m_tx_hash))).obj(),
      tx.m_public_key_known ? ScopedJavaLocalRef<jstring>(
          env, NativeToJavaString(env, pod_to_hex(tx.m_public_key))).obj()
                            : nullptr,
      tx.m_key_image_known ? ScopedJavaLocalRef<jstring>(
          env, NativeToJavaString(env, pod_to_hex(tx.m_key_image))).obj()
                           : nullptr,
      tx.m_subaddress_major,
      tx.m_subaddress_minor,
      (!tx.m_recipient.empty()) ? ScopedJavaLocalRef<jstring>(
          env, NativeToJavaString(env, tx.m_recipient)).obj()
                                : nullptr,
      tx.m_amount,
      static_cast<jint>(tx.m_height),
      tx.m_unlock_time,
      tx.m_timestamp,
      tx.m_fee,
      tx.m_change,
      static_cast<jbyte>(tx.m_state),
      tx.m_coinbase,
      tx.m_type == TxInfo::INCOMING)
  };
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeGetTxHistory(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  jobjectArray j_array;
  wallet->withTxHistory([env, &j_array](std::vector<TxInfo> const& txs) {
    j_array = NativeToJavaObjectArray<TxInfo>(env,
                                              txs,
                                              TxInfoClass.obj(),
                                              &NativeToJavaTxInfo);
  });
  return j_array;
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeCreatePayment(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jobjectArray j_addresses,
    jlongArray j_amounts,
    jint priority,
    jint account_index,
    jintArray j_subaddr_indexes,
    jobject j_callback) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);

  const auto& addresses = JavaToNativeVector<std::string, jstring>(
      env, j_addresses, &JavaToNativeString);
  const auto& amounts = JavaToNativeLongArray(env, j_amounts);
  const auto& subaddr_indexes = JavaToNativeIntArray(env, j_subaddr_indexes);

  std::unique_ptr<PendingTransfer> pending_transfer;

  try {
    pending_transfer = wallet->createPayment(
        addresses,
        {amounts.begin(), amounts.end()},
        priority,
        account_index,
        {subaddr_indexes.begin(), subaddr_indexes.end()});
//  } catch (error::daemon_busy& e) {
//  } catch (error::no_connection_to_daemon& e) {
//  } catch (error::wallet_rpc_error& e) {
//  } catch (error::get_outs_error& e) {
//  } catch (error::not_enough_unlocked_money& e) {
//  } catch (error::not_enough_money& e) {
//  } catch (error::tx_not_possible& e) {
//  } catch (error::not_enough_outs_to_mix& e) {
//  } catch (error::tx_not_constructed& e) {
//  } catch (error::tx_rejected& e) {
//  } catch (error::tx_sum_overflow& e) {
//  } catch (error::zero_amount& e) {
//  } catch (error::zero_destination& e) {
//  } catch (error::tx_too_big& e) {
//  } catch (error::transfer_error& e) {
//  } catch (error::wallet_internal_error& e) {
//  } catch (error::wallet_logic_error& e) {
  } catch (const std::exception& e) {
    LOGW("Caught unhandled exception: %s", e.what());
    CallVoidMethod(env, j_callback,
                   ITransferCallback_onUnexpectedError,
                   NativeToJavaString(env, e.what()));
    return;
  }

  PendingTransfer* ptr = pending_transfer.release();

  jobject j_pending_transfer = CallObjectMethod(
      env, thiz,
      NativeWallet_createPendingTransfer,
      NativeToJavaPointer(ptr),
      ptr->amount(),
      ptr->fee(),
      ptr->txCount());

  CallVoidMethod(env, j_callback,
                 ITransferCallback_onTransferCreated, j_pending_transfer);
}

extern "C"
JNIEXPORT void JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeCommitPendingTransfer(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jlong transfer_handle,
    jobject j_callback) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  auto* pending_transfer = reinterpret_cast<PendingTransfer*>(transfer_handle);

  try {
    wallet->commit_transfer(*pending_transfer);
  } catch (const std::exception& e) {
    LOGW("Caught unhandled exception: %s", e.what());
    CallVoidMethod(env, j_callback,
                   ITransferCallback_onUnexpectedError,
                   NativeToJavaString(env, e.what()));
    return;
  }

  CallVoidMethod(env, j_callback, ITransferCallback_onTransferCommitted);
}

extern "C"
JNIEXPORT jlongArray JNICALL
Java_im_molly_monero_sdk_internal_NativeWallet_nativeFetchBaseFeeEstimate(
    JNIEnv* env,
    jobject thiz,
    jlong handle) {
  auto* wallet = reinterpret_cast<Wallet*>(handle);
  std::vector<uint64_t> fees = wallet->fetchBaseFeeEstimate();
  return NativeToJavaLongArray(env, fees.data(), fees.size());
}

}  // namespace monero
