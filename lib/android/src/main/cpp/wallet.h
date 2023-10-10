#ifndef WALLET_H_
#define WALLET_H_

#include <ostream>

#include "http_client.h"
#include "jvm.h"

#include "wallet2.h"

namespace monero {

using transfer_details = tools::wallet2::transfer_details;
using payment_details = tools::wallet2::payment_details;
using pool_payment_details = tools::wallet2::pool_payment_details;
using confirmed_transfer_details = tools::wallet2::confirmed_transfer_details;
using unconfirmed_transfer_details = tools::wallet2::unconfirmed_transfer_details;

// Basic structure combining transaction details with input or output info.
struct TxInfo {
  crypto::hash m_tx_hash;
  crypto::public_key m_key;
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
  bool m_key_image_known;

  enum TxType {
    INCOMING = 0,
    OUTGOING = 1,
  } m_type;

  enum TxState {
    OFF_CHAIN = 0,
    PENDING = 1,
    FAILED = 2,
    ON_CHAIN = 3,
  } m_state;

  TxInfo(crypto::hash tx_hash, TxType type):
      m_tx_hash(tx_hash),
      m_key(crypto::public_key{}),
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
      m_key_image_known(false),
      m_type(type),
      m_state(OFF_CHAIN) {}

  // TODO: Factory functions for various types of transactions.
};

// Wrapper for wallet2.h core API.
class Wallet : tools::i_wallet2_callback {
 public:
  enum Status : int {
    OK = 0,
    INTERRUPTED = 1,
    NO_NETWORK_CONNECTIVITY = 2,
    REFRESH_ERROR = 3,
  };

  Wallet(JNIEnv* env,
         int network_id,
         const JvmRef<jobject>& wallet_native);

  void restoreAccount(const std::vector<char>& secret_scalar, uint64_t restore_point);
  uint64_t estimateRestoreHeight(uint64_t timestamp);

  bool parseFrom(std::istream& input);
  bool writeTo(std::ostream& output);

  Wallet::Status nonReentrantRefresh(bool skip_coinbase);
  void cancelRefresh();
  void setRefreshSince(long height_or_timestamp);

  template<typename Consumer>
  void withTxHistory(Consumer consumer);

  std::string public_address() const;

  uint64_t current_blockchain_height() const { return m_blockchain_height; }

  // Extra state that must be persistent but isn't restored by wallet2's serializer.
  BEGIN_SERIALIZE_OBJECT()
    VERSION_FIELD(0)
    FIELD(m_restore_height)
  END_SERIALIZE()

 private:
  cryptonote::account_base& require_account();

  tools::wallet2 m_wallet;

  bool m_account_ready;
  uint64_t m_restore_height;
  uint64_t m_blockchain_height;

  // Saved transaction history.
  std::vector<TxInfo> m_tx_history;

  // Protects access to m_wallet instance and state fields.
  std::mutex m_wallet_mutex;
  std::mutex m_tx_history_mutex;
  std::mutex m_refresh_mutex;

  // Reference to Kotlin wallet instance.
  const ScopedJvmGlobalRef<jobject> m_callback;

  std::condition_variable m_refresh_cond;
  std::atomic<bool> m_refresh_running;
  bool m_refresh_canceled;
  bool m_balance_changed;

  void notifyRefresh(bool debounce);

  template<typename T>
  auto suspendRefreshAndRunLocked(T block) -> decltype(block());

  void captureTxHistorySnapshot(std::vector<TxInfo>& snapshot);
  void handleNewBlock(uint64_t height, bool refresh_running);
  void handleReorgEvent(uint64_t at_block_height);
  void handleMoneyEvent(uint64_t at_block_height);

  // Implementation of i_wallet2_callback follows.
 private:
  void on_new_block(uint64_t height, const cryptonote::block& block) override {
    handleNewBlock(height, true);
  }

  void on_reorg(uint64_t height) override {
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

#endif  // WALLET_H_
