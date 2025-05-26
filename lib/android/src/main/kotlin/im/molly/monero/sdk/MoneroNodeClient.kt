package im.molly.monero.sdk

import im.molly.monero.sdk.internal.IHttpRpcClient
import im.molly.monero.sdk.internal.RpcClient
import im.molly.monero.sdk.loadbalancer.FirstRule
import im.molly.monero.sdk.loadbalancer.LoadBalancer
import im.molly.monero.sdk.loadbalancer.Rule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
        fun create(
            network: MoneroNetwork,
            remoteNodes: Flow<List<RemoteNode>>,
            loadBalancerRule: Rule,
            httpClient: OkHttpClient = OkHttpClient(),
            retryBackoff: BackoffPolicy = ExponentialBackoff.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): MoneroNodeClient {
            val scope = CoroutineScope(ioDispatcher + SupervisorJob())
            val loadBalancer = LoadBalancer(remoteNodes.enforceNetwork(network), scope)
            val rpcClient = RpcClient(
                loadBalancer = loadBalancer,
                loadBalancerRule = loadBalancerRule,
                retryBackoff = retryBackoff,
                requestsScope = scope,
                httpClient = httpClient,
            )
            return MoneroNodeClient(network, rpcClient, scope)
        }

        private fun Flow<List<RemoteNode>>.enforceNetwork(
            expected: MoneroNetwork,
        ): Flow<List<RemoteNode>> = map { nodes ->
            val firstMismatch = nodes.firstOrNull { it.network != expected }
            require(firstMismatch == null) {
                "Received remote nodes for a different network: $firstMismatch (expected $expected)"
            }
            nodes
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

/**
 * Creates a [MoneroNodeClient] that connects only to this [RemoteNode].
 */
fun RemoteNode.singleNodeClient(
    httpClient: OkHttpClient = OkHttpClient(),
    retryBackoff: BackoffPolicy = ExponentialBackoff.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): MoneroNodeClient =
    MoneroNodeClient.create(
        network = network,
        remoteNodes = flowOf(listOf(this)),
        loadBalancerRule = FirstRule,
        httpClient = httpClient,
        retryBackoff = retryBackoff,
        ioDispatcher = ioDispatcher,
    )
