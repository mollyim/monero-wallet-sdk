package im.molly.monero.sdk

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface RestorePoint {
    fun toLong(): Long

    companion object {
        val GenesisBlock: RestorePoint = RestorePointValue(heightOrTimestamp = 0)

        fun blockHeight(height: Int): RestorePoint {
            require(isBlockHeightInRange(height))
            return RestorePointValue(heightOrTimestamp = height.toLong())
        }

        fun creationTime(localDate: LocalDate): RestorePoint = creationTime(
            epochSecond = localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        )

        fun creationTime(instant: Instant): RestorePoint = creationTime(
            epochSecond = instant.epochSecond
        )

        fun creationTime(epochSecond: Long): RestorePoint {
            require(epochSecond >= 1402185600) {
                "Monero accounts cannot be restored before 2014-06-08"
            }
            return RestorePointValue(heightOrTimestamp = epochSecond)
        }
    }
}

@JvmInline
value class RestorePointValue(val heightOrTimestamp: Long) : RestorePoint {
    override fun toLong() = heightOrTimestamp
}
