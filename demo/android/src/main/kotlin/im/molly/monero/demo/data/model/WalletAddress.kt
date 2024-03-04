package im.molly.monero.demo.data.model

import im.molly.monero.AccountAddress
import im.molly.monero.Enote
import im.molly.monero.TimeLocked

data class WalletAddress(
    val address: AccountAddress,
    val enotes: List<TimeLocked<Enote>>,
    val used: Boolean,
    val isLastForAccount: Boolean,
)
