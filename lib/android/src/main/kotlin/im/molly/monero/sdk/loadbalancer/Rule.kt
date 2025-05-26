package im.molly.monero.sdk.loadbalancer

import im.molly.monero.sdk.RemoteNode
import java.util.concurrent.atomic.AtomicInteger

interface Rule {
    /**
     * Returns one alive [RemoteNode] from the [loadBalancer] using its internal
     * node selection rule, or null if none are available.
     */
    fun chooseNode(loadBalancer: LoadBalancer): RemoteNode?
}

object FirstRule : Rule {
    override fun chooseNode(loadBalancer: LoadBalancer): RemoteNode? {
        return loadBalancer.onlineNodes.firstOrNull()
    }
}

class RoundRobinRule : Rule {
    private var currentIndex = AtomicInteger(0)

    override fun chooseNode(loadBalancer: LoadBalancer): RemoteNode? {
        val nodes = loadBalancer.onlineNodes
        return if (nodes.isNotEmpty()) {
            val index = currentIndex.getAndIncrement() % nodes.size
            nodes[index]
        } else {
            null
        }
    }
}
