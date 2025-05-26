package im.molly.monero.sdk

import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize

@Parcelize
data class RemoteNode(
    val url: String,
    val network: MoneroNetwork,
    val username: String? = null,
    val password: String? = null,
) : Parcelable {

    fun uriForPath(path: String): Uri =
        url.toUri().buildUpon().appendPath(path.trimStart('/')).build()

    override fun toString(): String {
        val masked = if (password == null) "null" else "***"
        return "RemoteNode(url=$url, network=$network, username=$username, password=$masked)"
    }
}
