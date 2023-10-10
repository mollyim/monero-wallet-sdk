package im.molly.monero

@JvmInline
value class PublicKey(private val publicKey: String) {
    init {
        require(publicKey.length == 64) { "Public key length must be 64 bytes" }
    }
}
