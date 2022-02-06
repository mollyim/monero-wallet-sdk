package im.molly.monero

import android.content.Intent
import android.os.IBinder
import android.os.ParcelFileDescriptor
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
        client: IRemoteNodeClient?,
    ): IWallet {
        randomSecretKey().use { secretKey ->
            return createOrRestoreWallet(config, client, secretKey)
        }
    }

    override fun restoreWallet(
        config: WalletConfig?,
        client: IRemoteNodeClient?,
        secretSpendKey: SecretKey?,
        accountCreationTimestamp: Long,
    ): IWallet {
        secretSpendKey.use { secretKey ->
            return createOrRestoreWallet(config, client, secretKey, accountCreationTimestamp)
        }
    }

    private fun createOrRestoreWallet(
        config: WalletConfig?,
        client: IRemoteNodeClient?,
        secretSpendKey: SecretKey?,
        accountCreationTimestamp: Long? = null,
    ): IWallet {
        requireNotNull(config)
        requireNotNull(secretSpendKey)
        return WalletNative.fullNode(
            networkId = config.networkId,
            secretSpendKey = secretSpendKey,
            remoteNodeClient = client,
            accountTimestamp = accountCreationTimestamp,
            coroutineContext = serviceScope.coroutineContext,
        )
    }

    override fun openWallet(
        config: WalletConfig?,
        client: IRemoteNodeClient?,
        source: ParcelFileDescriptor?,
    ): IWallet {
        requireNotNull(config)
        requireNotNull(source)
        return WalletNative.fullNode(
            networkId = config.networkId,
            savedDataFd = source.fd,
            remoteNodeClient = client,
            coroutineContext = serviceScope.coroutineContext,
        )
    }

    override fun print(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        listener?.onLogMessage(priority, tag, msg, tr?.toString())
    }
}
