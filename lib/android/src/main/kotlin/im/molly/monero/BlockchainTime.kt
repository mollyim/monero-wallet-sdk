package im.molly.monero

import android.os.Parcelable
import im.molly.monero.internal.constants.DIFFICULTY_TARGET_V2
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.Instant
import java.time.LocalDate


/**
 * A point in the blockchain timeline, which could be either a block height or a timestamp.
 */
@Parcelize
open class BlockchainTime(
    val height: Int,
    val timestamp: Instant,
) : Comparable<BlockchainTime>, Parcelable {

    init {
        require(isBlockHeightInRange(height)) {
            "Block height $height out of range"
        }
    }

    open fun toLong(): Long = height.toLong()

    override fun compareTo(other: BlockchainTime): Int =
        this.height.compareTo(other.height)

    override fun toString(): String = "Block $height | Time $timestamp"

    data object Genesis : BlockchainTime(0, Instant.ofEpochSecond(1397818193))

    class Block(height: Int, referencePoint: BlockchainTime = Genesis) :
        BlockchainTime(height, estimateTimestamp(height, referencePoint)) {

        override fun toString(): String = "Block $height | Time $timestamp (Estimated)"
    }

    class Timestamp(timestamp: Instant, referencePoint: BlockchainTime = Genesis) :
        BlockchainTime(estimateBlockHeight(timestamp, referencePoint), timestamp) {

        constructor(date: LocalDate) : this(Instant.ofEpochSecond(date.toEpochDay()))

        override fun toLong() = timestamp.epochSecond.coerceAtLeast(BlockHeader.MAX_HEIGHT + 1L)

        override fun compareTo(other: BlockchainTime): Int =
            this.timestamp.compareTo(other.timestamp)

        override fun toString(): String = "Block $height (Estimated) | Time $timestamp"
    }

    companion object {
        val AVERAGE_BLOCK_TIME: Duration = Duration.ofSeconds(DIFFICULTY_TARGET_V2)

        fun estimateTimestamp(targetHeight: Int, referencePoint: BlockchainTime): Instant {
            require(targetHeight >= 0) {
                "Block height $targetHeight must not be negative"
            }

            return when (targetHeight) {
                0 -> Genesis.timestamp
                else -> {
                    val heightDiff = targetHeight - referencePoint.height
                    val estTimeDiff = AVERAGE_BLOCK_TIME.multipliedBy(heightDiff.toLong())
                    referencePoint.timestamp.plus(estTimeDiff)
                }
            }
        }

        fun estimateBlockHeight(targetTime: Instant, referencePoint: BlockchainTime): Int {
            val timeDiff = Duration.between(referencePoint.timestamp, targetTime)
            val estHeight = timeDiff.seconds / AVERAGE_BLOCK_TIME.seconds + referencePoint.height
            val clampedHeight = estHeight.coerceIn(0, BlockHeader.MAX_HEIGHT.toLong())
            return clampedHeight.toInt()
        }
    }

    fun resolveUnlockTime(heightOrTimestamp: Long): BlockchainTime {
        return if (isBlockHeightInRange(heightOrTimestamp)) {
            val height = heightOrTimestamp.toInt()
            Block(height, referencePoint = this)
        } else {
            val clampedTs =
                if (heightOrTimestamp < 0 || heightOrTimestamp > Instant.MAX.epochSecond) Instant.MAX
                else Instant.ofEpochSecond(heightOrTimestamp)
            Timestamp(clampedTs, referencePoint = this)
        }
    }

    fun until(endTime: BlockchainTime): BlockchainTimeSpan {
        return BlockchainTimeSpan(
            duration = Duration.between(timestamp, endTime.timestamp),
            blocks = endTime.height - height,
        )
    }

    operator fun minus(other: BlockchainTime): BlockchainTimeSpan = other.until(this)
}

fun max(a: BlockchainTime, b: BlockchainTime) = if (a >= b) a else b

fun min(a: BlockchainTime, b: BlockchainTime) = if (a <= b) a else b

data class BlockchainTimeSpan(val duration: Duration, val blocks: Int) {
    val timeRemaining: Duration
        get() = maxOf(Duration.ZERO, duration)

    companion object {
        val ZERO = BlockchainTimeSpan(duration = Duration.ZERO, blocks = 0)
    }
}

class TimeLocked<T>(val value: T, val unlockTime: BlockchainTime?) {
    fun isLocked(currentTime: BlockchainTime): Boolean {
        return currentTime < (unlockTime ?: return false)
    }

    fun isUnlocked(currentTime: BlockchainTime) = !isLocked(currentTime)

    fun timeUntilUnlock(currentTime: BlockchainTime): BlockchainTimeSpan {
        return if (isLocked(currentTime)) {
            unlockTime!!.minus(currentTime)
        } else {
            BlockchainTimeSpan.ZERO
        }
    }
}
