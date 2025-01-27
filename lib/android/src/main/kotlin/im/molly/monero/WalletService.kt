package im.molly.monero

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.molly.monero.internal.IHttpRpcClient
import kotlinx.coroutines.*

class WalletService : LifecycleService() {
    private lateinit var service: IWalletService

    override fun onCreate() {
        super.onCreate()
        service = WalletServiceImpl(application.isIsolatedProcess(), lifecycleScope)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return service.asBinder()
    }
}

internal class WalletServiceImpl(
    isIsolated: Boolean,
    private val serviceScope: CoroutineScope,
) : IWalletService.Stub(), LogAdapter {

    private val logger = loggerFor<WalletService>()

    init {
        if (isIsolated) {
            setLoggingAdapter(this)
        }
        NativeLoader.loadWalletLibrary(logger = logger)
    }

    private var listener: IWalletServiceListener? = null

    override fun setListener(l: IWalletServiceListener) {
        listener = l
    }

    override fun createWallet(
        config: WalletConfig,
        storage: IStorageAdapter,
        rpcClient: IHttpRpcClient?,
        callback: IWalletServiceCallbacks?,
    ) {
        serviceScope.launch {
            val secretSpendKey = randomSecretKey()
            val wallet = secretSpendKey.use { secret ->
                createOrRestoreWallet(config, storage, rpcClient, secret)
            }
            callback?.onWalletResult(wallet)
        }
    }

    override fun restoreWallet(
        config: WalletConfig,
        storage: IStorageAdapter,
        rpcClient: IHttpRpcClient?,
        callback: IWalletServiceCallbacks?,
        secretSpendKey: SecretKey,
        restorePoint: Long,
    ) {
        serviceScope.launch {
            val wallet = secretSpendKey.use { secret ->
                createOrRestoreWallet(config, storage, rpcClient, secret, restorePoint)
            }
            callback?.onWalletResult(wallet)
        }
    }

    override fun openWallet(
        config: WalletConfig,
        storage: IStorageAdapter,
        rpcClient: IHttpRpcClient?,
        callback: IWalletServiceCallbacks?,
    ) {
        serviceScope.launch {
            val wallet = WalletNative.localSyncWallet(
                networkId = config.networkId,
                storageAdapter = storage,
                rpcClient = rpcClient,
                coroutineContext = serviceScope.coroutineContext,
            )
            callback?.onWalletResult(wallet)
        }
    }

    private suspend fun createOrRestoreWallet(
        config: WalletConfig,
        storage: IStorageAdapter,
        rpcClient: IHttpRpcClient?,
        secretSpendKey: SecretKey,
        restorePoint: Long? = null,
    ): IWallet {
        return WalletNative.localSyncWallet(
            networkId = config.networkId,
            storageAdapter = storage,
            rpcClient = rpcClient,
            secretSpendKey = secretSpendKey,
            restorePoint = restorePoint,
            coroutineContext = serviceScope.coroutineContext,
        )
    }

    override fun print(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        listener?.onLogMessage(priority, tag, msg, tr?.toString())
    }
}
