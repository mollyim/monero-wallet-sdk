package im.molly.monero

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RemoteNode(
    val uri: Uri,
    val username: String? = null,
    val password: String? = null,
) : Parcelable {
    fun uriForPath(path: String): Uri =
        uri.buildUpon().appendPath(path.trimStart('/')).build()
}
