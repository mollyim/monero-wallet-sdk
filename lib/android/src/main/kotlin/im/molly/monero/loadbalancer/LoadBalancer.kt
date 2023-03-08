package im.molly.monero.loadbalancer

import im.molly.monero.RemoteNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.time.Duration

class LoadBalancer(
    remoteNodes: Flow<List<RemoteNode>>,
    scope: CoroutineScope,
) {
    var onlineNodes: List<RemoteNode> = emptyList()

    init {
        scope.launch {
            remoteNodes.collect {
                updateNodes(it)
            }
        }
    }

    private fun updateNodes(nodeList: List<RemoteNode>) {
        onlineNodes = nodeList
    }

    fun onResponseTimeObservation(remoteNode: RemoteNode, responseTime: Duration) {
        // TODO
    }
}

sealed interface RemoteNodeState {
    /**
     * The remote node is currently online and able to handle requests.
     */
    data class Online(val responseTime: Duration) : RemoteNodeState

    /**
     * The client's request has timed out and no response has been received.
     */
    data class Timeout(val cause: Throwable?)

    /**
     * Indicates that an error occurred while processing the client's request to the remote node.
     */
//    open data class Error(val message: String?) : RemoteNodeState {

    /**
     * Indicates that the client is unauthorized to access the remote node, i.e. the client's credentials were invalid.
     */
//    data class Unauthorized(override val message: String?) : Error
}
