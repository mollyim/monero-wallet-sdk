package im.molly.monero

@JvmInline
value class HashDigest(private val hashDigest: String) {
    init {
        require(hashDigest.length == 32) { "Hash length must be 32 bytes" }
    }
}
