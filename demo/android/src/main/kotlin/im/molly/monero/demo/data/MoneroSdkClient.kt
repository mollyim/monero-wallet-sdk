package im.molly.monero.demo.data

import android.content.Context
import im.molly.monero.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class MoneroSdkClient(
    private val context: Context,
    private val walletDataFileStorage: WalletDataFileStorage,
    private val httpClient: OkHttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun createWallet(moneroNetwork: MoneroNetwork): MoneroWallet =
        withContext(ioDispatcher) {
            val walletClient = WalletClient.forNetwork(
                context = context,
                network = moneroNetwork,
            )
            val wallet = walletClient.createNewWallet()
            walletDataFileStorage.tryWriteData(wallet.publicAddress, false) { output ->
                walletClient.saveWallet(wallet, output)
            }
            wallet
        }

    suspend fun openWallet(
        publicAddress: String,
        remoteNodeSelector: RemoteNodeSelector?,
    ): MoneroWallet =
        withContext(ioDispatcher) {
            val walletClient = WalletClient.forNetwork(
                context = context,
                network = MoneroNetwork.of(publicAddress),
                nodeSelector = remoteNodeSelector,
                httpClient = httpClient,
            )
            walletDataFileStorage.readData(publicAddress).use { input ->
                walletClient.openWallet(input)
            }
        }
}
