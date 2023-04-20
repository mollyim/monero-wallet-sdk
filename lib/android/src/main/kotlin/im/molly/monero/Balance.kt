package im.molly.monero

data class Balance(
    private val spendableTxOuts: Set<OwnedTxOut>,
) {
    val totalAmount: AtomicAmount = spendableTxOuts.sumOf { it.amount }

    fun totalAmountUnlockedAt(
        blockHeight: Long,
        timestampMillis: Long = System.currentTimeMillis()
        // TODO: Create Timelock class
    ): AtomicAmount {
        require(blockHeight > 0)
        require(timestampMillis >= 0)
        TODO()
    }

    companion object {
        fun of(txOuts: List<OwnedTxOut>) = Balance(
            spendableTxOuts = txOuts.filter { it.notSpent }.toSet(),
        )
    }
}
