package im.molly.monero

import android.os.Parcelable
import im.molly.monero.internal.constants.CRYPTONOTE_DEFAULT_TX_SPENDABLE_AGE
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.Instant

/**
 * A point in the blockchain timeline, which could be either a block height or a timestamp.
 */
@Parcelize
data class BlockchainTime(
    val height: Int,
    val timestamp: Instant,
    val network: MoneroNetwork,
) : RestorePoint, Parcelable {

    constructor(blockHeader: BlockHeader, network: MoneroNetwork) : this(
        blockHeader.height, blockHeader.timestamp, network
    )

    init {
        require(isBlockHeightInRange(height)) {
            "Block height $height out of range"
        }

        require(isBlockEpochInRange(timestamp.epochSecond)) {
            "Block timestamp $timestamp out of range"
        }
    }

    fun estimateHeight(targetTimestamp: Instant): Int {
        val timeDiff = Duration.between(timestamp, targetTimestamp)
        val estHeight = timeDiff.seconds / network.avgBlockTime(height).seconds + height
        val validHeight = estHeight.coerceIn(0, BlockHeader.MAX_HEIGHT.toLong())
        return validHeight.toInt()
    }

    fun estimateTimestamp(targetHeight: Int): Instant {
        require(targetHeight >= 0) {
            "Block height $targetHeight must not be negative"
        }

        val heightDiff = targetHeight - height
        val estTimeDiff = network.avgBlockTime(height).multipliedBy(heightDiff.toLong())
        return timestamp.plus(estTimeDiff)
    }

    fun effectiveUnlockTime(targetHeight: Int, txTimeLock: UnlockTime?): UnlockTime {
        val spendableHeight = targetHeight + CRYPTONOTE_DEFAULT_TX_SPENDABLE_AGE - 1
        val spendableTime =  BlockchainTime(
            height = spendableHeight,
            timestamp = estimateTimestamp(spendableHeight),
            network = network,
        )

        return txTimeLock?.takeIf { it > spendableTime } ?: spendableTime.toUnlockTime()
    }

    private fun toUnlockTime(): UnlockTime {
        return UnlockTime.Block(blockchainTime = this)
    }

    fun until(endTime: BlockchainTime): BlockchainTimeSpan = BlockchainTimeSpan(
        duration = Duration.between(timestamp, endTime.timestamp),
        blocks = endTime.height - height,
    )

    operator fun minus(other: BlockchainTime): BlockchainTimeSpan = other.until(this)

    override fun toString(): String = "Block $height | Time $timestamp"

    override fun toLong() = height.toLong()
}

val MoneroNetwork.genesisTime: BlockchainTime
    get() = BlockchainTime(1, Instant.ofEpochSecond(epoch), this)

val MoneroNetwork.v2forkTime: BlockchainTime
    get() = BlockchainTime(epochV2.first, Instant.ofEpochSecond(epochV2.second), this)

fun MoneroNetwork.estimateTimestamp(targetHeight: Int): Instant {
    return if (targetHeight < v2forkTime.height) {
        genesisTime.estimateTimestamp(targetHeight)
    } else {
        v2forkTime.estimateTimestamp(targetHeight)
    }
}

fun MoneroNetwork.estimateHeight(targetTimestamp: Instant): Int {
    return if (targetTimestamp < v2forkTime.timestamp) {
        genesisTime.estimateHeight(targetTimestamp)
    } else {
        v2forkTime.estimateHeight(targetTimestamp)
    }
}

data class BlockchainTimeSpan(val duration: Duration, val blocks: Int) {
    val timeRemaining: Duration
        get() = maxOf(Duration.ZERO, duration)

    companion object {
        val ZERO = BlockchainTimeSpan(duration = Duration.ZERO, blocks = 0)
    }
}

sealed interface UnlockTime : Comparable<BlockchainTime>, Parcelable {
    val blockchainTime: BlockchainTime

    @Parcelize
    data class Block(override val blockchainTime: BlockchainTime) : UnlockTime {
        override operator fun compareTo(other: BlockchainTime): Int {
            return blockchainTime.height.compareTo(other.height)
        }
    }

    @Parcelize
    data class Timestamp(override val blockchainTime: BlockchainTime) : UnlockTime {
        override operator fun compareTo(other: BlockchainTime): Int {
            return blockchainTime.timestamp.compareTo(other.timestamp)
        }
    }
}
