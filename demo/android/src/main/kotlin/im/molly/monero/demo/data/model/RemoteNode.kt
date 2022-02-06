package im.molly.monero.demo.data.model

import android.net.Uri
import im.molly.monero.MoneroNetwork

data class RemoteNode(
    val id: Long? = null,
    val network: MoneroNetwork,
    val uri: Uri,
    val username: String = "",
    val password: String = "",
) {
    companion object {
        val EMPTY = RemoteNode(network = DefaultMoneroNetwork, uri = Uri.EMPTY)
    }
}

val DefaultMoneroNetwork = MoneroNetwork.Mainnet
