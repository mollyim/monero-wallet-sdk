package im.molly.monero

data class Balance(
    val pendingBalance: AtomicAmount,
    val timeLockedAmounts: Set<TimeLocked<AtomicAmount>>,
) {
    val confirmedBalance: AtomicAmount = timeLockedAmounts.sumOf { it.value }

    fun unlockedBalance(currentTime: BlockchainTime): AtomicAmount =
        timeLockedAmounts
            .mapNotNull { it.getValueIfUnlocked(currentTime) }
            .sum()

    fun lockedBalance(currentTime: BlockchainTime): Map<BlockchainTimeSpan, AtomicAmount> =
        timeLockedAmounts
            .filter { it.isLocked(currentTime) }
            .groupBy({ it.timeUntilUnlock(currentTime) }, { it.value })
            .mapValues { (_, amounts) -> amounts.sum() }
}

fun Iterable<TimeLocked<Enote>>.balance(subAccountSelector: (Int) -> Boolean = { true }): Balance {
    val enotes = filter { subAccountSelector(it.value.owner.accountIndex) }
    val (pending, confirmed) = enotes.partition { it.value.age == 0 }

    val timeLockedSet = confirmed
        .groupBy({ it.unlockTime }, { it.value.amount })
        .map { (unlockTime, amounts) -> TimeLocked(amounts.sum(), unlockTime) }
        .toSet()

    return Balance(pending.sumOf { it.value.amount }, timeLockedSet)
}
