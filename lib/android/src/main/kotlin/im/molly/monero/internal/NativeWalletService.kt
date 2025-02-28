package im.molly.monero.internal

import im.molly.monero.IStorageAdapter
import im.molly.monero.IWallet
import im.molly.monero.LogAdapter
import im.molly.monero.NativeLoader
import im.molly.monero.SecretKey
import im.molly.monero.WalletConfig
import im.molly.monero.WalletNative
import im.molly.monero.loggerFor
import im.molly.monero.randomSecretKey
import im.molly.monero.setLoggingAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class NativeWalletService(
    private val serviceScope: CoroutineScope,
) : IWalletService.Stub(), LogAdapter {

    private val logger = loggerFor<NativeWalletService>()

    init {
        NativeLoader.loadWalletLibrary(logger = logger)
    }

    fun configureLoggingAdapter() {
        setLoggingAdapter(this)
    }

    private var listener: IWalletServiceListener? = null

    override fun setListener(l: IWalletServiceListener) {
        listener = l
    }

    override fun print(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        listener?.onLogMessage(priority, tag, msg, tr?.toString())
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
}
