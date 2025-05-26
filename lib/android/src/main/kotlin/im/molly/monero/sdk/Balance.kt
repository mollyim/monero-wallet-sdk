package im.molly.monero.sdk

data class Balance(
    val lockableAmounts: List<TimeLocked<MoneroAmount>>,
    val pendingAmount: MoneroAmount = MoneroAmount.ZERO,
) {
    val confirmedAmount: MoneroAmount = lockableAmounts.sumOf { it.value }
    val totalAmount: MoneroAmount = confirmedAmount + pendingAmount

    companion object {
        val EMPTY = Balance(emptyList())
    }

    fun unlockedAmountAt(targetTime: BlockchainTime): MoneroAmount {
        return lockableAmounts
            .filter { it.isUnlocked(targetTime) }
            .sumOf { it.value }
    }

    fun lockedAmountsAt(targetTime: BlockchainTime): Map<BlockchainTimeSpan, MoneroAmount> {
        return lockableAmounts
            .filter { it.isLocked(targetTime) }
            .groupBy({ it.timeUntilUnlock(targetTime) }, { it.value })
            .mapValues { it.value.sum() }
    }
}

fun Iterable<TimeLocked<Enote>>.calculateBalance(
    accountFilter: (owner: AccountAddress) -> Boolean = { true },
): Balance {
    val lockableAmounts = mutableListOf<TimeLocked<MoneroAmount>>()

    var pendingAmount = MoneroAmount.ZERO

    for (timeLocked in filter { !it.value.spent && accountFilter(it.value.owner) }) {
        if (timeLocked.value.age == 0) {
            pendingAmount += timeLocked.value.amount
        } else {
            lockableAmounts.add(TimeLocked(timeLocked.value.amount, timeLocked.unlockTime))
        }
    }

    return Balance(lockableAmounts, pendingAmount)
}
