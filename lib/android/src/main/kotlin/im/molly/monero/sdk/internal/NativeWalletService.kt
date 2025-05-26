package im.molly.monero.sdk.internal

import android.app.Service
import android.os.ParcelFileDescriptor
import im.molly.monero.sdk.LogAdapter
import im.molly.monero.sdk.SecretKey
import im.molly.monero.sdk.isIsolatedProcess
import im.molly.monero.sdk.randomSecretKey
import im.molly.monero.sdk.setLoggingAdapter
import kotlinx.coroutines.CoroutineScope

internal class NativeWalletService(
    private val service: Service,
    private val serviceScope: CoroutineScope,
) : IWalletService.Stub(), LogAdapter {

    private val logger = loggerFor<NativeWalletService>()

    init {
        NativeLoader.loadWalletLibrary(logger = logger)
        if (isServiceIsolated) {
            setLoggingAdapter(this)
        }
    }

    private var listener: IWalletServiceListener? = null

    override fun setListener(l: IWalletServiceListener) {
        listener = l
    }

    override fun isServiceIsolated(): Boolean = service.application.isIsolatedProcess()

    override fun createWallet(
        config: WalletConfig,
        rpcClient: IHttpRpcClient?,
        callback: IWalletServiceCallbacks?,
    ) {
        val secretSpendKey = randomSecretKey()
        val wallet = secretSpendKey.use { secret ->
            createOrRestoreWallet(config, rpcClient, secret)
        }
        callback?.onWalletResult(wallet)
    }

    override fun restoreWallet(
        config: WalletConfig,
        rpcClient: IHttpRpcClient?,
        callback: IWalletServiceCallbacks?,
        secretSpendKey: SecretKey,
        restorePoint: Long,
    ) {
        val wallet = secretSpendKey.use { secret ->
            createOrRestoreWallet(config, rpcClient, secret, restorePoint)
        }
        callback?.onWalletResult(wallet)
    }

    override fun openWallet(
        config: WalletConfig,
        rpcClient: IHttpRpcClient?,
        callback: IWalletServiceCallbacks?,
        inputFd: ParcelFileDescriptor,
    ) {
        val wallet = inputFd.use {
            NativeWallet.localSyncWallet(
                networkId = config.networkId,
                rpcClient = rpcClient,
                walletDataFd = inputFd,
                coroutineContext = serviceScope.coroutineContext,
            )
        }
        callback?.onWalletResult(wallet)
    }

    private fun createOrRestoreWallet(
        config: WalletConfig,
        rpcClient: IHttpRpcClient?,
        secretSpendKey: SecretKey,
        restorePoint: Long? = null,
    ): IWallet {
        return NativeWallet.localSyncWallet(
            networkId = config.networkId,
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
