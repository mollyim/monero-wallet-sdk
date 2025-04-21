package im.molly.monero.demo.data

import android.content.Context
import android.util.AtomicFile
import im.molly.monero.*
import im.molly.monero.loadbalancer.RoundRobinRule
import im.molly.monero.service.SandboxedWalletService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MoneroSdkClient(private val context: Context) {

    private val providerDeferred = CoroutineScope(Dispatchers.IO).async {
        SandboxedWalletService.connect(context)
    }

    suspend fun createWallet(network: MoneroNetwork, filename: String): MoneroWallet {
        val provider = providerDeferred.await()
        return provider.createNewWallet(
            network = network,
            dataStore = WalletDataStoreFile(filename, newFile = true),
        ).also { wallet ->
            wallet.save()
        }
    }

    suspend fun restoreWallet(
        network: MoneroNetwork,
        filename: String,
        secretSpendKey: SecretKey,
        restorePoint: RestorePoint,
    ): MoneroWallet {
        val provider = providerDeferred.await()
        return provider.restoreWallet(
            network = network,
            dataStore = WalletDataStoreFile(filename, newFile = true),
            secretSpendKey = secretSpendKey,
            restorePoint = restorePoint,
        ).also { wallet ->
            wallet.save()
        }
    }

    suspend fun openWallet(
        network: MoneroNetwork,
        filename: String,
        remoteNodes: Flow<List<RemoteNode>>,
        httpClient: OkHttpClient,
    ): MoneroWallet {
        val dataStore = WalletDataStoreFile(filename)
        val client = MoneroNodeClient.forNetwork(
            network = network,
            remoteNodes = remoteNodes,
            loadBalancerRule = RoundRobinRule(),
            httpClient = httpClient,
        )
        val provider = providerDeferred.await()
        return provider.openWallet(network, dataStore, client)
    }

    private val filesDir = context.filesDir

    private inner class WalletDataStoreFile(filename: String, newFile: Boolean = false) :
        WalletDataStore {

        private val file: AtomicFile = getBackingFile(filename)

        init {
            if (newFile && !file.baseFile.createNewFile()) {
                throw IOException("Data file already exists: ${file.baseFile.path}")
            }
        }

        private fun getBackingFile(filename: String): AtomicFile =
            AtomicFile(File(getOrCreateWalletDataDir(), "$filename.wallet"))

        private fun getOrCreateWalletDataDir(): File {
            val walletDataDir = File(filesDir, "wallet_data")
            if (walletDataDir.exists() || walletDataDir.mkdir()) {
                return walletDataDir
            }
            throw IOException("Cannot create wallet data directory: ${walletDataDir.path}")
        }

        override suspend fun save(writer: (OutputStream) -> Unit, overwrite: Boolean) {
            val output = file.startWrite()
            try {
                writer(output)
                file.finishWrite(output)
            } catch (ioe: IOException) {
                file.failWrite(output)
                throw ioe
            }
        }

        override suspend fun load(): InputStream {
            return file.openRead()
        }
    }
}
