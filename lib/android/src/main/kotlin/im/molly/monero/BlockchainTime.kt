package im.molly.monero

import im.molly.monero.internal.constants.DIFFICULTY_TARGET_V2
import java.time.Duration
import java.time.Instant
import java.time.LocalDate


/**
 * A point in the blockchain timeline, which could be either a block height or a timestamp.
 */
open class BlockchainTime(
    val height: Int,
    val timestamp: Instant,
) : Comparable<BlockchainTime> {

    init {
        require(isBlockHeightInRange(height)) {
            "Block height $height out of range"
        }
    }

    open fun toLong(): Long = height.toLong()

    override fun compareTo(other: BlockchainTime): Int =
        this.height.compareTo(other.height)

    data object Genesis : BlockchainTime(0, Instant.ofEpochSecond(1397818193))

    class Block(height: Int, currentTime: BlockchainTime = Genesis) :
        BlockchainTime(height, estimateTimestamp(height, currentTime))

    class Timestamp(timestamp: Instant, currentTime: BlockchainTime = Genesis) :
        BlockchainTime(estimateBlockHeight(timestamp, currentTime), timestamp) {

        constructor(date: LocalDate) : this(Instant.ofEpochSecond(date.toEpochDay()))

        override fun toLong() = timestamp.epochSecond.coerceAtLeast(BlockHeader.MAX_HEIGHT + 1L)

        override fun compareTo(other: BlockchainTime): Int =
            this.timestamp.compareTo(other.timestamp)
    }

    companion object {
        val AVERAGE_BLOCK_TIME = Duration.ofSeconds(DIFFICULTY_TARGET_V2)

        fun estimateTimestamp(targetHeight: Int, currentTime: BlockchainTime): Instant {
            require(targetHeight >= 0) { "Block height $targetHeight must not be negative" }

            return if (targetHeight == 0) {
                Genesis.timestamp
            } else {
                val heightDiff = targetHeight - currentTime.height
                val estTimeDiff = AVERAGE_BLOCK_TIME.multipliedBy(heightDiff.toLong())
                currentTime.timestamp.plus(estTimeDiff)
            }
        }

        fun estimateBlockHeight(targetTime: Instant, currentTime: BlockchainTime): Int {
            val timeDiff = Duration.between(currentTime.timestamp, targetTime)
            val estHeight = timeDiff.seconds / AVERAGE_BLOCK_TIME.seconds + currentTime.height
            val clampedHeight = estHeight.coerceIn(0, BlockHeader.MAX_HEIGHT.toLong())
            return clampedHeight.toInt()
        }
    }

    fun fromUnlockTime(heightOrTimestamp: Long): BlockchainTime {
        return if (isBlockHeightInRange(heightOrTimestamp)) {
            Block(heightOrTimestamp.toInt(), currentTime = this)
        } else {
            val clampedTs =
                if (heightOrTimestamp < 0 || heightOrTimestamp > Instant.MAX.epochSecond) Instant.MAX
                else Instant.ofEpochSecond(heightOrTimestamp)
            Timestamp(clampedTs, currentTime = this)
        }
    }

    fun until(endTime: BlockchainTime): BlockchainTimeSpan {
        return BlockchainTimeSpan(
            duration = Duration.between(timestamp, endTime.timestamp),
            blocks = endTime.height - height,
        )
    }

    operator fun minus(other: BlockchainTime): BlockchainTimeSpan = until(other)
}

data class BlockchainTimeSpan(val duration: Duration, val blocks: Int) {
    companion object {
        val ZERO = BlockchainTimeSpan(duration = Duration.ZERO, blocks = 0)
    }
}

class TimeLocked<T>(val value: T, val unlockTime: BlockchainTime) {
    fun isLocked(currentTime: BlockchainTime): Boolean = currentTime < unlockTime

    fun getValueIfUnlocked(currentTime: BlockchainTime): T? {
        return if (isLocked(currentTime)) null else value
    }

    fun timeUntilUnlock(currentTime: BlockchainTime): BlockchainTimeSpan {
        return if (isLocked(currentTime)) {
            unlockTime.minus(currentTime)
        } else {
            BlockchainTimeSpan.ZERO
        }
    }
}
