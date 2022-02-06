package im.molly.monero

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import java.io.Closeable
import java.security.MessageDigest
import java.security.SecureRandom
import javax.security.auth.Destroyable

/**
 * Elliptic curve secret key.
 *
 * SecretKey wraps a secret scalar value, helping to prevent accidental exposure and securely
 * erasing the value from memory.
 */
@Parcelize
class SecretKey : Destroyable, Closeable, Parcelable {

    private val secret = ByteArray(32)

    constructor() {
        SecureRandom().nextBytes(secret)
    }

    constructor(secretScalar: ByteArray) {
        require(secretScalar.size == 32) { "Secret key must be 256 bits" }
        secretScalar.copyInto(secret)
    }

    private constructor(parcel: Parcel) {
        parcel.readByteArray(secret)
    }

    companion object : Parceler<SecretKey> {
        override fun create(parcel: Parcel) = SecretKey(parcel)

        override fun SecretKey.write(parcel: Parcel, flags: Int) {
            parcel.writeByteArray(secret)
        }
    }

    val bytes: ByteArray
        get() {
            check(!destroyed) { "Secret key has been already destroyed" }
            check(isNonZero) { "Secret key cannot be zero" }
            return secret.clone()
        }

    val isNonZero
        get() = !MessageDigest.isEqual(secret, ByteArray(secret.size))

    var destroyed = false
        private set

    override fun destroy() {
        if (!destroyed) {
            secret.fill(0)
            destroyed = true
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SecretKey) return false

        return MessageDigest.isEqual(secret, other.secret)
    }

    override fun hashCode(): Int {
        return secret.contentHashCode()
    }

    override fun close() = destroy()

    protected fun finalize() = destroy()
}

fun randomSecretKey(): SecretKey = SecretKey()
