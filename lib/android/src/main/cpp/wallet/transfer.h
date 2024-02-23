#ifndef WALLET_TRANSFER_H_
#define WALLET_TRANSFER_H_

#include "wallet2.h"

namespace monero {

using wallet2 = tools::wallet2;

class PendingTransfer {
 public:
  PendingTransfer(const std::vector<wallet2::pending_tx>& ptxs)
      : m_ptxs(ptxs) {}

 private:
  std::vector<wallet2::pending_tx> m_ptxs;
};

}  // namespace monero

#endif  // WALLET_TRANSFER_H_
