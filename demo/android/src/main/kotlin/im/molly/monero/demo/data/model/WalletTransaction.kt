package im.molly.monero.demo.data.model

import im.molly.monero.sdk.Transaction

data class WalletTransaction(
    val walletId: Long,
    val transaction: Transaction,
)
