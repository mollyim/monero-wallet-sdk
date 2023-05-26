package im.molly.monero

import java.time.Instant
import java.time.LocalDate

class RestorePoint {
    val heightOrTimestamp: Long

    constructor() {
        heightOrTimestamp = 0
    }

    constructor(blockHeight: Long) {
        require(blockHeight >= 0) { "Block height cannot be negative" }
        require(blockHeight < 500_000_000) { "Block height too large" }
        heightOrTimestamp = blockHeight
    }

    constructor(creationDate: LocalDate) {
        heightOrTimestamp = creationDate.toEpochDay().coerceAtLeast(500_000_000)
    }

    constructor(creationDate: Instant) {
        heightOrTimestamp = creationDate.epochSecond.coerceAtLeast(500_000_000)
    }
}
