package im.molly.monero

data class Ledger(
    val publicAddress: PublicAddress,
    val indexedAccounts: List<WalletAccount>,
    val transactionById: Map<String, Transaction>,
    val enoteSet: Set<TimeLocked<Enote>>,
    val checkedAt: BlockchainTime,
) {
    val transactions: Collection<Transaction>
        get() = transactionById.values

    val isBalanceZero: Boolean
        get() = getBalance().totalAmount.isZero

    fun getBalance(): Balance = enoteSet.calculateBalance()

    fun getBalanceForAccount(accountIndex: Int): Balance =
        enoteSet.calculateBalance { it.accountIndex == accountIndex }
}
