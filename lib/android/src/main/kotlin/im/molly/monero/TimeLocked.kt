package im.molly.monero

class TimeLocked<T>(val value: T, val unlockTime: UnlockTime?) {
    fun isLocked(currentTime: BlockchainTime): Boolean {
        return unlockTime != null && unlockTime > currentTime
    }

    fun isUnlocked(currentTime: BlockchainTime) = !isLocked(currentTime)

    fun timeUntilUnlock(currentTime: BlockchainTime): BlockchainTimeSpan {
        if (unlockTime == null || isUnlocked(currentTime)) {
            return BlockchainTimeSpan.ZERO
        }
        return unlockTime.blockchainTime - currentTime
    }
}
