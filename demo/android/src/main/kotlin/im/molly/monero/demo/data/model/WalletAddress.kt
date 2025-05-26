package im.molly.monero.demo.data.model

import im.molly.monero.sdk.AccountAddress
import im.molly.monero.sdk.Enote
import im.molly.monero.sdk.TimeLocked

data class WalletAddress(
    val address: AccountAddress,
    val enotes: List<TimeLocked<Enote>>,
    val used: Boolean,
    val isLastForAccount: Boolean,
)
