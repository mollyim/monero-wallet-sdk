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
        callback: IWalletServiceCallbacks?,
    ) {
        serviceScope.launch {
            val secretSpendKey = randomSecretKey()
            val wallet = secretSpendKey.use { secret ->
                createOrRestoreWallet(config, secret)
            }
            callback?.onWalletResult(wallet)
        }
    }

    override fun restoreWallet(
        config: WalletConfig?,
        callback: IWalletServiceCallbacks?,
        secretSpendKey: SecretKey?,
        accountCreationTimestamp: Long,
    ) {
        serviceScope.launch {
            val wallet = secretSpendKey.use { secret ->
                createOrRestoreWallet(config, secret, accountCreationTimestamp)
            }
            callback?.onWalletResult(wallet)
        }
    }

    override fun openWallet(
        config: WalletConfig?,
        callback: IWalletServiceCallbacks?,
    ) {
        requireNotNull(config)
        serviceScope.launch {
            val wallet = WalletNative.fullNode(
                networkId = config.networkId,
                storageAdapter = config.storageAdapter,
                remoteNodeClient = config.remoteNodeClient,
                coroutineContext = serviceScope.coroutineContext,
            )
            callback?.onWalletResult(wallet)
        }
    }

    private fun createOrRestoreWallet(
        config: WalletConfig?,
        secretSpendKey: SecretKey?,
        accountCreationTimestamp: Long? = null,
    ): IWallet {
        requireNotNull(config)
        requireNotNull(secretSpendKey)
        return WalletNative.fullNode(
            networkId = config.networkId,
            storageAdapter = config.storageAdapter,
            remoteNodeClient = config.remoteNodeClient,
            secretSpendKey = secretSpendKey,
            accountTimestamp = accountCreationTimestamp,
            coroutineContext = serviceScope.coroutineContext,
        )
    }

    override fun print(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        listener?.onLogMessage(priority, tag, msg, tr?.toString())
    }
}
