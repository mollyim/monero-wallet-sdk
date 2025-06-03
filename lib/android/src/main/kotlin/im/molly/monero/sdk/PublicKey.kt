package im.molly.monero.sdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@JvmInline
@OptIn(ExperimentalStdlibApi::class)
value class PublicKey(private val hexString: String) : Parcelable {
    init {
        require(hexString.length == 64) {
            "Public key length must be 32 bytes (64 hex chars)"
        }
        require(hexString.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            "Public key must be a valid hex string"
        }
    }

    constructor(publicKeyBytes: ByteArray) : this(publicKeyBytes.toHexString())

    val bytes: ByteArray
        get() = hexString.hexToByteArray()

    override fun toString(): String = hexString
}
