package im.molly.monero.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.VisibleForTesting
import im.molly.monero.BlockchainTime
import im.molly.monero.MoneroNetwork
import im.molly.monero.MoneroNodeClient
import im.molly.monero.MoneroWallet
import im.molly.monero.RestorePoint
import im.molly.monero.SecretKey
import im.molly.monero.WalletDataStore
import im.molly.monero.WalletProvider
import im.molly.monero.service.BaseWalletService
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

internal class WalletServiceClient(
    private val context: Context,
    private val service: IWalletService,
    private val serviceConnection: ServiceConnection?,
) : WalletProvider {

    companion object {
        suspend fun bindService(
            context: Context,
            serviceClass: Class<out BaseWalletService>,
        ): WalletServiceClient {
            val (serviceConnection, service) = bindServiceAwait(context, serviceClass)
            return WalletServiceClient(context, service, serviceConnection)
        }

        private suspend fun bindServiceAwait(
            context: Context,
            serviceClass: Class<out BaseWalletService>,
        ): Pair<ServiceConnection, IWalletService> {
            val deferredService = CompletableDeferred<IWalletService>()
            val serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val service = IWalletService.Stub.asInterface(binder).apply {
                        setListener(WalletServiceLogListener)
                    }
                    deferredService.complete(service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    deferredService.completeExceptionally(WalletProvider.ServiceNotBoundException())
                }
            }

            val intent = Intent(context, serviceClass)
            if (!context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
                throw WalletProvider.ServiceNotBoundException()
            }

            return serviceConnection to deferredService.await()
        }

        @VisibleForTesting
        fun withBoundService(context: Context, service: IWalletService): WalletServiceClient {
            return WalletServiceClient(context, service, serviceConnection = null)
        }
    }

    private val logger = loggerFor<WalletServiceClient>()

    override suspend fun createNewWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore?,
        client: MoneroNodeClient?,
    ): MoneroWallet {
        validateClientNetwork(client, network)
        val wallet = suspendCancellableCoroutine { continuation ->
            service.createWallet(
                buildConfig(network), client?.httpRpcClient,
                WalletResultCallback(continuation),
            )
        }
        val storeAdapter = dataStore?.let { DataStoreAdapter(it) }
        return MoneroWallet(wallet, storeAdapter, client)
    }

    override suspend fun restoreWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore?,
        client: MoneroNodeClient?,
        secretSpendKey: SecretKey,
        restorePoint: RestorePoint,
    ): MoneroWallet {
        validateClientNetwork(client, network)
        validateRestorePoint(restorePoint, network)
        val wallet = suspendCancellableCoroutine { continuation ->
            service.restoreWallet(
                buildConfig(network), client?.httpRpcClient,
                WalletResultCallback(continuation),
                secretSpendKey,
                restorePoint.toLong(),
            )
        }
        val storeAdapter = dataStore?.let { DataStoreAdapter(it) }
        return MoneroWallet(wallet, storeAdapter, client)
    }

    override suspend fun openWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore,
        client: MoneroNodeClient?,
    ): MoneroWallet {
        validateClientNetwork(client, network)
        val storeAdapter = DataStoreAdapter(dataStore)
        return storeAdapter.loadWithFd { fd ->
            val wallet = suspendCancellableCoroutine { continuation ->
                service.openWallet(
                    buildConfig(network), client?.httpRpcClient,
                    WalletResultCallback(continuation),
                    fd,
                )
            }
            MoneroWallet(wallet, storeAdapter, client)
        }
    }

    private fun buildConfig(network: MoneroNetwork): WalletConfig {
        return WalletConfig(network.id)
    }

    private fun validateClientNetwork(client: MoneroNodeClient?, network: MoneroNetwork) {
        require(client == null || client.network == network) {
            "Client network mismatch: expected $network, got ${client?.network}"
        }
    }

    private fun validateRestorePoint(restorePoint: RestorePoint, network: MoneroNetwork) {
        if (restorePoint is BlockchainTime) {
            require(restorePoint.network == network) {
                "Restore point network mismatch: expected $network, got ${restorePoint.network}"
            }
        }
    }

    override fun isServiceSandboxed(): Boolean =
        service.isRemote() && service.isServiceIsolated

    override fun disconnect() {
        context.unbindService(serviceConnection ?: return)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private class WalletResultCallback(
        private val continuation: CancellableContinuation<IWallet>,
    ) : IWalletServiceCallbacks.Stub() {
        override fun onWalletResult(wallet: IWallet?) {
            if (wallet != null) {
                continuation.resume(wallet) { wallet.close() }
            } else {
                TODO()
            }
        }
    }
}
