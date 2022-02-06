package im.molly.monero

data class Balance(
    private val uTxOuts: Set<OwnedTxOut>,
) {
    val totalAmount: Amount = uTxOuts.sumOf { it.amount }

    fun totalAmountUnlockedAt(
        blockHeight: Long,
        timestampMillis: Long = System.currentTimeMillis()
    ): Amount {
        require(blockHeight > 0)
        require(timestampMillis >= 0)
        TODO()
    }

    companion object {
        fun of(txOuts: List<OwnedTxOut>) = Balance(
            uTxOuts = txOuts.filter { it.notSpent }.toSet(),
        )
    }
}
