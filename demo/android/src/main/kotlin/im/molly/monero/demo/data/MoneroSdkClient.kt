package im.molly.monero.demo.data

import android.content.Context
import im.molly.monero.*
import im.molly.monero.loadbalancer.RoundRobinRule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient

class MoneroSdkClient(
    private val context: Context,
    private val walletDataFileStorage: WalletDataFileStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val providerDeferred = CoroutineScope(ioDispatcher).async {
        WalletProvider.connect(context)
    }

    suspend fun createWallet(moneroNetwork: MoneroNetwork): MoneroWallet {
        val provider = providerDeferred.await()
        return withContext(ioDispatcher) {
            val wallet = provider.createNewWallet(moneroNetwork)
            walletDataFileStorage.tryWriteData(wallet.publicAddress, false) { output ->
                provider.saveWallet(wallet, output)
            }
            wallet
        }
    }

    suspend fun openWallet(
        publicAddress: String,
        remoteNodes: Flow<List<RemoteNode>>,
        httpClient: OkHttpClient,
    ): MoneroWallet {
        val provider = providerDeferred.await()
        return withContext(ioDispatcher) {
            val network = MoneroNetwork.of(publicAddress)
            val client = RemoteNodeClient.forNetwork(
                network = network,
                remoteNodes = remoteNodes,
                loadBalancerRule = RoundRobinRule(),
                httpClient = httpClient,
            )
            val wallet = walletDataFileStorage.readData(publicAddress).use { input ->
                provider.openWallet(network, client, input)
            }
            check(publicAddress == wallet.primaryAccountAddress) { "primary address mismatch" }
            wallet
        }
    }
}
