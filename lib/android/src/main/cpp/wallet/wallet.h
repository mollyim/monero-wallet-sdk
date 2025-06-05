#ifndef WALLET_WALLET_H_
#define WALLET_WALLET_H_

#include <ostream>

#include "common/jvm.h"

#include "transfer.h"
#include "http_client.h"

#include "wallet2.h"

namespace monero {

namespace error = tools::error;

using wallet2 = tools::wallet2;
using i_wallet2_callback = tools::i_wallet2_callback;

// Basic structure combining transaction details with input or output info.
struct TxInfo {
  crypto::hash m_tx_hash;
  crypto::public_key m_public_key;
  crypto::key_image m_key_image;
  uint32_t m_subaddress_major;
  uint32_t m_subaddress_minor;
  std::string m_recipient;
  uint64_t m_amount;
  uint64_t m_height;
  uint64_t m_unlock_time;
  uint64_t m_timestamp;
  uint64_t m_fee;
  uint64_t m_change;
  bool m_coinbase;
  bool m_public_key_known;
  bool m_key_image_known;

  enum TxType {
    INCOMING = 0,
    OUTGOING = 1,
  } m_type;

  enum TxState {
    OFF_CHAIN = 1,
    PENDING = 2,
    FAILED = 3,
    ON_CHAIN = 4,
  } m_state;

  TxInfo(crypto::hash tx_hash, TxType type) :
      m_tx_hash(tx_hash),
      m_public_key(crypto::public_key{}),
      m_key_image(crypto::key_image{}),
      m_subaddress_major(-1),
      m_subaddress_minor(-1),
      m_recipient(),
      m_amount(0),
      m_height(0),
      m_unlock_time(0),
      m_timestamp(0),
      m_fee(0),
      m_change(0),
      m_coinbase(false),
      m_public_key_known(false),
      m_key_image_known(false),
      m_type(type),
      m_state(OFF_CHAIN) {}

  // TODO: Factory functions for various types of transactions.
};

// Wrapper for wallet2.h core API.
class Wallet : i_wallet2_callback {
 public:
  enum Status : int {
    OK = 0,
    INTERRUPTED = 1,
    NO_NETWORK_CONNECTIVITY = 2,
    REFRESH_ERROR = 3,
  };

 public:
  Wallet(JNIEnv* env,
         int network_id,
         const JavaRef<jobject>& wallet_native);

  void restoreAccount(const std::vector<char>& secret_scalar, uint64_t restore_point);
  uint64_t estimateRestoreHeight(uint64_t timestamp);

  bool parseFrom(std::istream& input);
  bool writeTo(std::ostream& output);

  Wallet::Status nonReentrantRefresh(bool skip_coinbase);
  void cancelRefresh();
  void setRefreshSince(long height_or_timestamp);

  std::string addDetachedSubAddress(uint32_t index_major, uint32_t index_minor);
  std::string createSubAddressAccount();
  std::string createSubAddress(uint32_t index_major);

  std::unique_ptr<PendingTransfer> createPayment(
      const std::vector<std::string>& addresses,
      const std::vector<uint64_t>& amounts,
      int priority,
      uint32_t account_index,
      const std::set<uint32_t>& subaddr_indexes);

  void commit_transfer(PendingTransfer& pending_transfer);

  template<typename Consumer>
  void withTxHistory(Consumer consumer);

  std::vector<uint64_t> fetchBaseFeeEstimate();

  std::string public_address() const;
  std::vector<std::string> formatted_subaddresses(uint32_t index_major = -1);

  crypto::secret_key spend_secret_key() const;
  crypto::secret_key view_secret_key() const;

  uint32_t current_blockchain_height() const { return static_cast<uint32_t>(m_last_block_height); }
  uint64_t current_blockchain_timestamp() const { return m_last_block_timestamp; }

  // Extra state that must be persistent but isn't restored by wallet2's serializer.
  BEGIN_SERIALIZE_OBJECT()
    VERSION_FIELD(0)
    FIELD(m_restore_height)
    FIELD(m_last_block_height)
    FIELD(m_last_block_timestamp)
  END_SERIALIZE()

 private:
  cryptonote::account_base& require_account();
  const cryptonote::account_base& require_account() const;

  wallet2 m_wallet;

  bool m_account_ready;
  uint64_t m_restore_height;
  uint64_t m_last_block_height;
  uint64_t m_last_block_timestamp;

  std::map<cryptonote::subaddress_index, std::string> m_subaddresses;

  // Saved transaction history.
  std::vector<TxInfo> m_tx_history;

  // Protects access to m_wallet instance and state fields.
  std::mutex m_wallet_mutex;
  std::mutex m_tx_history_mutex;
  std::mutex m_subaddresses_mutex;

  // Reference to Kotlin wallet instance.
  const ScopedJavaGlobalRef<jobject> m_callback;

  std::condition_variable m_refresh_cond;
  std::atomic<bool> m_refresh_running;
  bool m_refresh_canceled;
  bool m_balance_changed;

  void processBalanceChanges(bool refresh_running);
  void notifyRefreshState(bool debounce);

  template<typename T>
  auto suspendRefreshAndRunLocked(T block) -> decltype(block());

  void captureTxHistorySnapshot(std::vector<TxInfo>& snapshot);
  void updateSubaddressMap(std::map<cryptonote::subaddress_index, std::string>& map);
  std::string addSubaddressInternal(const cryptonote::subaddress_index& index);
  void handleNewBlock(uint64_t height, uint64_t timestamp);
  void handleReorgEvent(uint64_t at_block_height);
  void handleMoneyEvent(uint64_t at_block_height);

  // Implementation of i_wallet2_callback follows.
 private:
  void on_new_block(uint64_t height, const cryptonote::block& block) override {
    // Block could be empty during a fast refresh.
    handleNewBlock(height, block.timestamp);
  }

  void on_reorg(uint64_t height, uint64_t blocks_detached, size_t transfers_detached) override {
    handleReorgEvent(height);
  }

  void on_money_received(uint64_t height,
                         const crypto::hash& txid,
                         const cryptonote::transaction& tx,
                         uint64_t amount,
                         uint64_t burnt,
                         const cryptonote::subaddress_index& subaddr_index,
                         bool is_change,
                         uint64_t unlock_time) override {
    handleMoneyEvent(height);
  }

  void on_unconfirmed_money_received(uint64_t height,
                                     const crypto::hash& txid,
                                     const cryptonote::transaction& tx,
                                     uint64_t amount,
                                     const cryptonote::subaddress_index& subaddr_index) override {
    handleMoneyEvent(height);
  }

  void on_money_spent(uint64_t height,
                      const crypto::hash& txid,
                      const cryptonote::transaction& in_tx,
                      uint64_t amount,
                      const cryptonote::transaction& spend_tx,
                      const cryptonote::subaddress_index& subaddr_index) override {
    handleMoneyEvent(height);
  };
};

}  // namespace monero

#endif  // WALLET_WALLET_H_
