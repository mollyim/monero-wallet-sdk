package im.molly.monero.demo.data.model

import im.molly.monero.AccountAddress

data class WalletAddress(
    val address: AccountAddress,
    val used: Boolean,
    val isLastForAccount: Boolean,
)
