package im.molly.monero.sdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
@OptIn(ExperimentalStdlibApi::class)
value class HashDigest(private val hexString: String) : Parcelable {
    init {
        require(hexString.length == 64) {
            "Hash length must be 32 bytes (64 hex chars)"
        }
        require(hexString.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            "Hash must be a valid hex string"
        }
    }

    constructor(hashDigestBytes: ByteArray) : this(hashDigestBytes.toHexString())

    val bytes: ByteArray
        get() = hexString.hexToByteArray()

    override fun toString(): String = hexString
}
