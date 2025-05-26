package im.molly.monero.sdk

data class TimeLocked<T>(val value: T, val unlockTime: UnlockTime? = null) {
    fun isLocked(currentTime: BlockchainTime): Boolean {
        val unlock = unlockTime ?: return false
        requireSameNetworkAs(currentTime)
        return unlock > currentTime
    }

    fun isUnlocked(currentTime: BlockchainTime) = !isLocked(currentTime)

    fun timeUntilUnlock(currentTime: BlockchainTime): BlockchainTimeSpan {
        if (unlockTime == null) return BlockchainTimeSpan.ZERO

        requireSameNetworkAs(currentTime)

        return if (unlockTime > currentTime) {
            unlockTime.blockchainTime - currentTime
        } else {
            BlockchainTimeSpan.ZERO
        }
    }

    private fun requireSameNetworkAs(other: BlockchainTime) {
        val expected = unlockTime?.blockchainTime?.network
        require(expected == other.network) {
            "BlockchainTime network mismatch: expected $expected, got ${other.network}"
        }
    }
}

fun MoneroAmount.lockedUntil(unlockTime: UnlockTime): TimeLocked<MoneroAmount> =
    TimeLocked(this, unlockTime)

fun MoneroAmount.unlocked(): TimeLocked<MoneroAmount> =
    TimeLocked(this)
