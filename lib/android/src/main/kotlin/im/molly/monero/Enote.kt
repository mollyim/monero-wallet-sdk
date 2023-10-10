package im.molly.monero

data class Enote(
    val amount: AtomicAmount,
    val owner: AccountAddress,
    val key: PublicKey,
    val keyImage: HashDigest?,
    val emissionTxId: String?,
    val age: Int,
) {
    init {
        require(age >= 0) { "Enote age $age must not be negative" }
    }
}
