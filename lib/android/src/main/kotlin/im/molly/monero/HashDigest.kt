package im.molly.monero

@JvmInline
value class HashDigest(private val hashDigest: String) {
    init {
        require(hashDigest.length == 64) { "Hash length must be 64 hex chars" }
    }

    override fun toString(): String = hashDigest
}
