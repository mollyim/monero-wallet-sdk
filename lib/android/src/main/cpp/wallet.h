#ifndef WALLET_H_
#define WALLET_H_

#include <iostream>

#include "http_client.h"
#include "jvm.h"

#include "wallet2.h"

namespace monero {

using TxOut = tools::wallet2::transfer_details;

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
         std::unique_ptr<HttpClientFactory> http_client_factory,
         const JvmRef<jobject>& callback);

  void restoreAccount(const std::vector<char>& secret_scalar, uint64_t account_timestamp);
  uint64_t estimateRestoreHeight(uint64_t timestamp);

  bool parseFrom(std::istream& input);
  bool writeTo(std::ostream& output);

  Wallet::Status refreshLoopUntilSynced(bool skip_coinbase);
  void stopRefresh();
  void setRefreshSince(long height_or_timestamp);

  template<typename Callback>
  void getOwnedTxOuts(Callback callback);

  std::string public_address() const;

  uint64_t current_blockchain_height() const { return m_blockchain_height; }

  bool refresh_is_running() const { return m_refresh_continue; }

  // Extra object's state that need to be persistent.
  BEGIN_SERIALIZE_OBJECT()
    VERSION_FIELD(0)
    FIELD(m_restore_height)
  END_SERIALIZE()

 private:
  cryptonote::account_base& require_account();

  tools::wallet2 m_wallet;

  std::vector<TxOut> m_tx_outs;
  bool m_account_ready;
  uint64_t m_restore_height;
  uint64_t m_blockchain_height;

  // Protects access to m_wallet instance and state fields.
  std::mutex m_wallet_mutex;
  std::mutex m_tx_outs_mutex;

  // Reference to the Wallet kotlin instance.
  const ScopedJvmGlobalRef<jobject> m_callback;

  std::condition_variable m_refresh_cond;
  bool m_refresh_started;
  bool m_refresh_continue;

  template<typename T>
  auto pauseRefreshAndRunLocked(T block) -> decltype(block());

  void handleBalanceChanged(uint64_t at_block_height);
  void handleNewBlock(uint64_t height);

  // Implementation of i_wallet2_callback follows.
 private:
  void on_new_block(uint64_t height, const cryptonote::block& block) override {
    handleNewBlock(height);
  }

  void on_reorg(uint64_t height, size_t blocks_detached, size_t transfers_detached) override {
    if (transfers_detached > 0) {
      handleBalanceChanged(height);
    }
  }

  void on_money_received(uint64_t height,
                         const crypto::hash& txid,
                         const cryptonote::transaction& tx,
                         uint64_t amount,
                         uint64_t burnt,
                         const cryptonote::subaddress_index& subaddr_index,
                         bool is_change,
                         uint64_t unlock_time) override {
    handleBalanceChanged(height);
  }

  void on_unconfirmed_money_received(uint64_t height,
                                     const crypto::hash& txid,
                                     const cryptonote::transaction& tx,
                                     uint64_t amount,
                                     const cryptonote::subaddress_index& subaddr_index) override {
    handleBalanceChanged(height);
  };

  void on_money_spent(uint64_t height,
                      const crypto::hash& txid,
                      const cryptonote::transaction& in_tx,
                      uint64_t amount,
                      const cryptonote::transaction& spend_tx,
                      const cryptonote::subaddress_index& subaddr_index) override {
    handleBalanceChanged(height);
  };
};

}  // namespace monero

#endif  // WALLET_H_
