package im.molly.monero

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
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
        MoneroJni.loadLibrary(logger = logger)
    }

    private var listener: IWalletServiceListener? = null

    override fun setListener(l: IWalletServiceListener) {
        listener = l
    }

    override fun createWallet(
        config: WalletConfig?,
        storage: IStorageAdapter?,
        client: IRemoteNodeClient?,
        callback: IWalletServiceCallbacks?,
    ) {
        serviceScope.launch {
            val secretSpendKey = randomSecretKey()
            val wallet = secretSpendKey.use { secret ->
                createOrRestoreWallet(config, storage, client, secret)
            }
            callback?.onWalletResult(wallet)
        }
    }

    override fun restoreWallet(
        config: WalletConfig?,
        storage: IStorageAdapter?,
        client: IRemoteNodeClient?,
        callback: IWalletServiceCallbacks?,
        secretSpendKey: SecretKey?,
        restorePoint: Long,
    ) {
        serviceScope.launch {
            val wallet = secretSpendKey.use { secret ->
                createOrRestoreWallet(config, storage, client, secret, restorePoint)
            }
            callback?.onWalletResult(wallet)
        }
    }

    override fun openWallet(
        config: WalletConfig?,
        storage: IStorageAdapter?,
        client: IRemoteNodeClient?,
        callback: IWalletServiceCallbacks?,
    ) {
        requireNotNull(config)
        requireNotNull(storage)
        serviceScope.launch {
            val wallet = WalletNative.fullNode(
                networkId = config.networkId,
                storageAdapter = storage,
                remoteNodeClient = client,
                coroutineContext = serviceScope.coroutineContext,
            )
            callback?.onWalletResult(wallet)
        }
    }

    private suspend fun createOrRestoreWallet(
        config: WalletConfig?,
        storage: IStorageAdapter?,
        client: IRemoteNodeClient?,
        secretSpendKey: SecretKey?,
        restorePoint: Long? = null,
    ): IWallet {
        requireNotNull(config)
        requireNotNull(storage)
        requireNotNull(secretSpendKey)
        return WalletNative.fullNode(
            networkId = config.networkId,
            storageAdapter = storage,
            remoteNodeClient = client,
            secretSpendKey = secretSpendKey,
            restorePoint = restorePoint,
            coroutineContext = serviceScope.coroutineContext,
        )
    }

    override fun print(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        listener?.onLogMessage(priority, tag, msg, tr?.toString())
    }
}
