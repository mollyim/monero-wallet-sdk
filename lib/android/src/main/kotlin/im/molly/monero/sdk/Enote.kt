package im.molly.monero.sdk

class Enote(
    val amount: MoneroAmount,
    val owner: AccountAddress,
    val key: PublicKey?,
    val keyImage: HashDigest?,
    val age: Int,
    val origin: EnoteOrigin,
) {
    init {
        require(amount > 0) { "Amount must be greater than 0" }
        require(age >= 0) { "Age cannot be negative" }
    }

    var spent: Boolean = false
        private set

    fun markAsSpent(): Enote {
        spent = true
        return this
    }

    val sourceTxId: String?
        get() = (origin as? EnoteOrigin.TxOut)?.txId

    override fun toString(): String {
        return "Enote(" +
                "amount=${amount.xmr}" +
                ", age=$age" +
                ", spent=$spent" +
                ", owner=$owner" +
                ", key=$key" +
                ", keyImage=$keyImage" +
                ", origin=$origin" +
                ")"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Enote) return false

        return amount == other.amount &&
                owner == other.owner &&
                key == other.key &&
                keyImage == other.keyImage &&
                age == other.age &&
                origin == other.origin
    }

    override fun hashCode(): Int {
        var result = age
        result = 31 * result + amount.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (keyImage?.hashCode() ?: 0)
        result = 31 * result + origin.hashCode()
        return result
    }
}

sealed interface EnoteOrigin {
    data class TxOut(val txId: String, val index: Int) : EnoteOrigin
}
