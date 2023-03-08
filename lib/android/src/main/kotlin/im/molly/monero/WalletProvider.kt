package im.molly.monero

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant

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

    fun createNewWallet(
        network: MoneroNetwork,
        client: RemoteNodeClient? = null,
    ): MoneroWallet {
        require(client == null || client.network == network)
        return MoneroWallet(
            service.createWallet(buildConfig(network), client), client
        )
    }

    fun restoreWallet(
        network: MoneroNetwork,
        client: RemoteNodeClient? = null,
        secretSpendKey: SecretKey,
        accountCreationTime: Instant,
    ): MoneroWallet {
        require(client == null || client.network == network)
        return MoneroWallet(
            service.restoreWallet(
                buildConfig(network),
                client,
                secretSpendKey,
                accountCreationTime.epochSecond,
            ),
            client,
        )
    }

    fun openWallet(
        network: MoneroNetwork,
        client: RemoteNodeClient? = null,
        source: FileInputStream,
    ): MoneroWallet =
        ParcelFileDescriptor.dup(source.fd).use { fd ->
            MoneroWallet(service.openWallet(buildConfig(network), client, fd), client)
        }

    fun saveWallet(wallet: MoneroWallet, destination: FileOutputStream) {
        ParcelFileDescriptor.dup(destination.fd).use {
            wallet.save(it)
        }
    }

    private fun buildConfig(network: MoneroNetwork) = WalletConfig(network.id)

    fun disconnect() {
        context.unbindService(serviceConnection)
    }
}

object WalletServiceListener : IWalletServiceListener.Stub() {
    override fun onLogMessage(priority: Int, tag: String?, msg: String?, cause: String?) {
        requireNotNull(tag)
        requireNotNull(msg)
        if (Logger.adapter.isLoggable(priority, tag)) {
            val tr = if (cause != null) Throwable(cause) else null
            Logger.adapter.print(priority, tag, msg, tr)
        }
    }
}
