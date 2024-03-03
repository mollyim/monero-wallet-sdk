#ifndef WALLET_TRANSFER_H_
#define WALLET_TRANSFER_H_

#include "wallet2.h"

namespace monero {

using wallet2 = tools::wallet2;

struct PendingTransfer {
  std::vector<wallet2::pending_tx> m_ptxs;

  uint64_t fee() const {
    uint64_t n = 0;
    for (const auto& ptx: m_ptxs) {
      n += ptx.fee;
    }
    return n;
  }

  uint64_t amount() const {
    uint64_t n = 0;
    for (const auto& ptx: m_ptxs) {
      for (const auto& dest: ptx.dests) {
        n += dest.amount;
      }
    }
    return n;
  }

  int txCount() const {
    return m_ptxs.size();
  }

  PendingTransfer(const std::vector<wallet2::pending_tx>& ptxs)
      : m_ptxs(ptxs) {}
};

}  // namespace monero

#endif  // WALLET_TRANSFER_H_
