package im.molly.monero

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class HashDigest(private val hashDigest: String) : Parcelable {
    init {
        require(hashDigest.length == 64) { "Hash length must be 64 hex chars" }
    }

    override fun toString(): String = hashDigest
}
