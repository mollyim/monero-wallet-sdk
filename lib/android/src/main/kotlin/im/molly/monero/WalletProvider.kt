package im.molly.monero

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.*

// TODO: Rename to SandboxedWalletProvider and extract interface, add InProcessWalletProvider
class WalletProvider private constructor(
    private val context: Context,
    private val service: IWalletService,
    private val serviceConnection: ServiceConnection,
// TODO: Remove DataStore dependencies if unused
//    private val dataStore: DataStore<WalletProto.State>,
) {
    companion object {
        suspend fun connect(context: Context): WalletProvider {
            val (serviceConnection, service) = bindService(context)
            return WalletProvider(context, service, serviceConnection)
        }

        private suspend fun bindService(context: Context): Pair<ServiceConnection, IWalletService> {
            val deferredService = CompletableDeferred<IWalletService>()
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val service = IWalletService.Stub.asInterface(binder)
                    service.setListener(WalletServiceListener)
                    deferredService.complete(service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    deferredService.completeExceptionally(ServiceNotBoundException())
                }
            }
            Intent(context, WalletService::class.java).also { intent ->
                if (!context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                    throw ServiceNotBoundException()
                }
            }
            return serviceConnection to deferredService.await()
        }
    }

    /** Exception thrown by [WalletProvider] if the remote service can't be bound. */
    class ServiceNotBoundException : Exception()

    private val logger = loggerFor<WalletProvider>()

    suspend fun createNewWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore? = null,
        client: RemoteNodeClient? = null,
    ): MoneroWallet {
        require(client == null || client.network == network)
        val storageAdapter = StorageAdapter(dataStore)
        val wallet = suspendCancellableCoroutine { continuation ->
            service.createWallet(
                buildConfig(network), storageAdapter, client,
                WalletResultCallback(continuation),
            )
        }
        return MoneroWallet(wallet, storageAdapter, client)
    }

    suspend fun restoreWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore? = null,
        client: RemoteNodeClient? = null,
        secretSpendKey: SecretKey,
        restorePoint: RestorePoint,
    ): MoneroWallet {
        require(client == null || client.network == network)
        if (restorePoint is BlockchainTime) {
            require(restorePoint.network == network)
        }
        val storageAdapter = StorageAdapter(dataStore)
        val wallet = suspendCancellableCoroutine { continuation ->
            service.restoreWallet(
                buildConfig(network), storageAdapter, client,
                WalletResultCallback(continuation),
                secretSpendKey,
                restorePoint.toLong(),
            )
        }
        return MoneroWallet(wallet, storageAdapter, client)
    }

    suspend fun openWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore,
        client: RemoteNodeClient? = null,
    ): MoneroWallet {
        require(client == null || client.network == network)
        val storageAdapter = StorageAdapter(dataStore)
        val wallet = suspendCancellableCoroutine { continuation ->
            service.openWallet(
                buildConfig(network), storageAdapter, client,
                WalletResultCallback(continuation),
            )
        }
        return MoneroWallet(wallet, storageAdapter, client)
    }

    private fun buildConfig(network: MoneroNetwork): WalletConfig {
        return WalletConfig(network.id)
    }

    fun disconnect() {
        context.unbindService(serviceConnection)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private class WalletResultCallback(
        private val continuation: CancellableContinuation<IWallet>
    ) : IWalletServiceCallbacks.Stub() {
        override fun onWalletResult(wallet: IWallet?) {
            when {
                wallet != null -> {
                    continuation.resume(wallet) {
                        wallet.close()
                    }
                }

                else -> TODO()
            }
        }
    }
}

object WalletServiceListener : IWalletServiceListener.Stub() {
    override fun onLogMessage(priority: Int, tag: String, msg: String, cause: String?) {
        if (Logger.adapter.isLoggable(priority, tag)) {
            val tr = if (cause != null) Throwable(cause) else null
            Logger.adapter.print(priority, tag, msg, tr)
        }
    }
}
