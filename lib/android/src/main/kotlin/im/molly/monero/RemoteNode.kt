package im.molly.monero

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.IOException

@Parcelize
data class RemoteNode(
    val uri: Uri,
    val username: String? = null,
    val password: String? = null,
) : Parcelable {
    fun uriForPath(path: String): Uri =
        uri.buildUpon().appendPath(path.trimStart('/')).build()
}

interface RemoteNodeSelector {
    /**
     * Selects an appropriate remote node to access the Monero network.
     */
    fun select(): RemoteNode?

    /**
     * Called to indicate that a connection could not be established to a remote node.
     *
     * An implementation of this method can temporarily remove the node or reorder the sequence
     * of nodes returned by [select].
     */
    fun connectFailed(remoteNode: RemoteNode, ioe: IOException)
}
