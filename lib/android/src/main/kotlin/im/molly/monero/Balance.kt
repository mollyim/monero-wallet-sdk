package im.molly.monero

data class Balance(
    val pendingAmount: MoneroAmount,
    val timeLockedAmounts: List<TimeLocked<MoneroAmount>>,
) {
    val confirmedAmount: MoneroAmount = timeLockedAmounts.sumOf { it.value }
    val totalAmount: MoneroAmount = confirmedAmount + pendingAmount

    fun unlockedAmountAt(targetTime: BlockchainTime): MoneroAmount {
        return timeLockedAmounts
            .filter { it.isUnlocked(targetTime) }
            .sumOf { it.value }
    }

    fun lockedAmountsAt(targetTime: BlockchainTime): Map<BlockchainTimeSpan, MoneroAmount> {
        return timeLockedAmounts
            .filter { it.isLocked(targetTime) }
            .groupBy({ it.timeUntilUnlock(targetTime) }, { it.value })
            .mapValues { (_, amounts) ->
                amounts.sum()
            }
    }
}

fun Iterable<TimeLocked<Enote>>.calculateBalance(): Balance {
    var pendingAmount = MoneroAmount.ZERO

    val lockedAmounts = mutableListOf<TimeLocked<MoneroAmount>>()

    for (timeLocked in filter { !it.value.spent }) {
        if (timeLocked.value.age == 0) {
            pendingAmount += timeLocked.value.amount
        } else {
            lockedAmounts.add(TimeLocked(timeLocked.value.amount, timeLocked.unlockTime))
        }
    }

    return Balance(pendingAmount, lockedAmounts)
}
