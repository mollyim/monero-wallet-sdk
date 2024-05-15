package im.molly.monero

import im.molly.monero.internal.IHttpRpcClient
import im.molly.monero.internal.RpcClient
import im.molly.monero.loadbalancer.LoadBalancer
import im.molly.monero.loadbalancer.Rule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient

class MoneroNodeClient private constructor(
    val network: MoneroNetwork,
    private val rpcClient: RpcClient,
    private val scope: CoroutineScope,
) : AutoCloseable {

    companion object {
        /**
         * Constructs a [MoneroNodeClient] to connect to the Monero [network].
         */
        fun forNetwork(
            network: MoneroNetwork,
            remoteNodes: Flow<List<RemoteNode>>,
            loadBalancerRule: Rule,
            httpClient: OkHttpClient,
            retryBackoff: BackoffPolicy = ExponentialBackoff.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): MoneroNodeClient {
            val scope = CoroutineScope(ioDispatcher + SupervisorJob())
            val loadBalancer = LoadBalancer(remoteNodes, scope)
            val rpcClient = RpcClient(
                loadBalancer = loadBalancer,
                loadBalancerRule = loadBalancerRule,
                retryBackoff = retryBackoff,
                requestsScope = scope,
                httpClient = httpClient,
            )
            return MoneroNodeClient(network, rpcClient, scope)
        }
    }

    var httpClient: OkHttpClient
        get() = rpcClient.httpClient
        set(value) {
            rpcClient.httpClient = value
        }

    internal val httpRpcClient: IHttpRpcClient
        get() = rpcClient

    override fun close() {
        scope.cancel("MoneroNodeClient is closing: Cancelling all ongoing requests")
    }
}
