package im.molly.monero.demo.data.model

import im.molly.monero.Transaction

data class WalletTransaction(
    val walletId: Long,
    val transaction: Transaction,
)
