package im.molly.monero

class Enote(
    val amount: MoneroAmount,
    val owner: AccountAddress,
    val key: PublicKey?,
    val keyImage: HashDigest?,
    val age: Int,
    val sourceTxId: String?,
) {
    init {
        require(age >= 0) { "Enote age $age must not be negative" }
    }

    var spent: Boolean = false
        private set

    fun markAsSpent() {
        spent = true
    }

    override fun hashCode() = System.identityHashCode(this)

    override fun equals(other: Any?) = this === other
}
