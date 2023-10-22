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
        require(amount > 0) { "Amount must be greater than 0" }
        require(age >= 0) { "Age cannot be negative" }
    }

    var spent: Boolean = false
        private set

    fun markAsSpent() {
        spent = true
    }

    override fun hashCode() = System.identityHashCode(this)

    override fun equals(other: Any?) = this === other

    override fun toString(): String {
        return "Enote(" +
                "amount=${amount.xmr}" +
                ", age=$age" +
                ", spent=$spent" +
                ", owner=$owner" +
                ", key=$key" +
                ", keyImage=$keyImage" +
                ", sourceTxId=$sourceTxId" +
                ")"
    }
}
