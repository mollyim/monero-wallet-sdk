package im.molly.monero

import im.molly.monero.internal.constants.DIFFICULTY_TARGET_V1
import im.molly.monero.internal.constants.DIFFICULTY_TARGET_V2
import java.time.Duration

/**
 * Monero network environments.
 *
 * Defined in cryptonote_config.h
 */
enum class MoneroNetwork(val id: Int, val epoch: Long, val epochV2: Pair<Int, Long>) {
    Mainnet(0, 1397818193, (1009827 to 1458748658)),
    Testnet(1, 1410295020, (624634 to 1448285909)),
    Stagenet(2, 1518932025, (32000 to 1520937818));

    companion object {
        fun fromId(value: Int) = entries.first { it.id == value }

        fun of(publicAddress: String) = PublicAddress.parse(publicAddress).network
    }

    fun avgBlockTime(height: Int): Duration {
        return if (height < epochV2.second) {
            Duration.ofSeconds(DIFFICULTY_TARGET_V1)
        } else {
            Duration.ofSeconds(DIFFICULTY_TARGET_V2)
        }
    }
}
