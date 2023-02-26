package im.molly.monero

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

// TODO: Rename to SandboxedWalletClient and extract interface, add InProcessWalletClient
class WalletClient private constructor(
    private val context: Context,
    private val service: IWalletService,
    private val serviceConnection: ServiceConnection,
    private val network: MoneroNetwork,
    private val remoteNodeClient: RemoteNodeClient?,
// TODO: Remove DataStore dependencies if unused
//    private val dataStore: DataStore<WalletProto.State>,
) : AutoCloseable {

    private val logger = loggerFor<WalletClient>()

    companion object {
        /**
         * Constructs a [WalletClient] to connect to the Monero network [network].
         *
         * @param context Calling application's [Context].
         * @throws [ServiceNotBoundException] if the wallet service can not be bound.
         */
        suspend fun forNetwork(
            context: Context,
            network: MoneroNetwork,
            nodeSelector: RemoteNodeSelector? = null,
            httpClient: OkHttpClient? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): WalletClient {
            val remoteNodeClient = nodeSelector?.let {
                requireNotNull(httpClient)
                RemoteNodeClient(it, httpClient, ioDispatcher)
            }
            val (serviceConnection, service) = bindService(context)
            return WalletClient(context, service, serviceConnection, network, remoteNodeClient)
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

    /** Exception thrown by [WalletClient] if the remote service can't be bound. */
    class ServiceNotBoundException : Exception()

    fun createNewWallet(): MoneroWallet =
        MoneroWallet(service.createWallet(buildConfig(), remoteNodeClient))

    fun restoreWallet(secretSpendKey: SecretKey, accountCreationTime: Instant): MoneroWallet =
        MoneroWallet(
            service.restoreWallet(
                buildConfig(),
                remoteNodeClient,
                secretSpendKey,
                accountCreationTime.epochSecond,
            )
        )

    fun openWallet(source: FileInputStream): MoneroWallet =
        ParcelFileDescriptor.dup(source.fd).use {
            MoneroWallet(service.openWallet(buildConfig(), remoteNodeClient, it))
        }

    fun saveWallet(wallet: MoneroWallet, destination: FileOutputStream) {
        ParcelFileDescriptor.dup(destination.fd).use {
            wallet.save(it)
        }
    }

    private fun buildConfig() = WalletConfig(network.id)

//    private fun <R> callRemoteService(task: () -> R): R = task()

    override fun close() {
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
