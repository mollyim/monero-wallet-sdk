package im.molly.monero

data class Enote(
    val amount: MoneroAmount,
    val owner: AccountAddress,
    val key: PublicKey,
    val keyImage: HashDigest?,
    val age: Int,
) {
    var spent: Boolean = false

    init {
        require(age >= 0) { "Enote age $age must not be negative" }
    }
}
