package im.molly.monero

import android.os.ParcelFileDescriptor
import androidx.annotation.GuardedBy
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

class WalletNative private constructor(
    networkId: Int,
    remoteNodeClient: IRemoteNodeClient?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : IWallet.Stub(), Closeable {

    companion object {
        fun fullNode(
            networkId: Int,
            secretSpendKey: SecretKey? = null,
            savedDataFd: Int? = null,
            accountTimestamp: Long? = null,
            remoteNodeClient: IRemoteNodeClient? = null,
            coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob(),
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = WalletNative(
            networkId = networkId,
            remoteNodeClient = remoteNodeClient,
            scope = CoroutineScope(coroutineContext),
            ioDispatcher = ioDispatcher,
        ).apply {
            secretSpendKey?.let { secretKey ->
                require(savedDataFd == null)
                require(accountTimestamp == null || accountTimestamp >= 0)
                val timestampOrNow = accountTimestamp ?: (System.currentTimeMillis() / 1000)
                nativeRestoreAccount(handle, secretKey.bytes, timestampOrNow)
            }

            savedDataFd?.let { fd ->
                require(secretSpendKey == null)
                require(accountTimestamp == null)
                if (!nativeLoad(handle, fd)) {
                    throw IllegalArgumentException("Cannot load wallet data")
                }
            }
        }
    }

    private val logger = loggerFor<WalletNative>()

    init {
        MoneroJni.loadLibrary(logger = logger)
    }

    private val handle: Long =
        nativeCreate(networkId, remoteNodeClient ?: IRemoteNodeClient.Default())

    override fun getPrimaryAccountAddress() = nativeGetPrimaryAccountAddress(handle)

    val currentBlockchainHeight: Long
        get() = nativeGetCurrentBlockchainHeight(handle)

    val currentBalance: Balance
        get() = Balance.of(ownedTxOutsSnapshot())

    private fun ownedTxOutsSnapshot(): List<OwnedTxOut> = nativeGetOwnedTxOuts(handle).toList()

    @GuardedBy("listenersLock")
    private val balanceListeners = mutableSetOf<IBalanceListener>()

    private val balanceListenersLock = ReentrantLock()

    private val refreshDispatcher = ioDispatcher.limitedParallelism(1)

    override fun restartRefresh(
        skipCoinbaseOutputs: Boolean,
        callback: IRefreshCallback?,
    ) {
        if (nativeRefreshIsRunning(handle)) {
            nativeStopRefresh(handle)
        }
        val refreshJob = scope.launch(refreshDispatcher) {
            val status = nativeRefreshLoopUntilSynced(handle, skipCoinbaseOutputs)
            callback?.onResult(currentBlockchainHeight, status)
        }
        // Spin until the refresh thread enters in a cancellable state
        while (refreshJob.isActive && !nativeRefreshIsRunning(handle)) {
            Thread.yield()
        }
    }

    override fun stopRefresh() = nativeStopRefresh(handle)

    override fun setRefreshSince(blockHeightOrTimestamp: Long) {
        nativeSetRefreshSince(handle, blockHeightOrTimestamp)
    }

    /**
     * Also replays the last known balance whenever a new listener registers.
     */
    override fun addBalanceListener(listener: IBalanceListener?) {
        requireNotNull(listener)
        balanceListenersLock.withLock {
            balanceListeners.add(listener)
            listener.onBalanceChanged(ownedTxOutsSnapshot(), currentBlockchainHeight)
        }
    }

    override fun removeBalanceListener(listener: IBalanceListener?) {
        requireNotNull(listener)
        balanceListenersLock.withLock {
            balanceListeners.remove(listener)
        }
    }

    override fun save(destination: ParcelFileDescriptor?) {
        requireNotNull(destination)
        nativeSave(handle, destination.fd)
    }

    @CalledByNative("wallet.cc")
    private fun onRefresh(blockchainHeight: Long, balanceChanged: Boolean) {
        balanceListenersLock.withLock {
            if (balanceListeners.isNotEmpty()) {
                val call = fun(listener: IBalanceListener) {
                    if (balanceChanged) {
                        val txOuts = ownedTxOutsSnapshot()
                        listener.onBalanceChanged(txOuts, blockchainHeight)
                    } else {
                        listener.onRefresh(blockchainHeight)
                    }
                }
                balanceListeners.forEach { call(it) }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }

    protected fun finalize() {
        nativeDispose(handle)
    }

    // Must match values in wallet.h
    internal object Status {
        const val OK: Int = 0
        const val INTERRUPTED: Int = 1
        const val NO_NETWORK_CONNECTIVITY: Int = 2
        const val REFRESH_ERROR: Int = 3
    }

    private external fun nativeCreate(networkId: Int, remoteNodeClient: IRemoteNodeClient): Long
    private external fun nativeDispose(handle: Long)
    private external fun nativeGetCurrentBlockchainHeight(handle: Long): Long
    private external fun nativeGetOwnedTxOuts(handle: Long): Array<OwnedTxOut>
    private external fun nativeGetPrimaryAccountAddress(handle: Long): String
    private external fun nativeLoad(handle: Long, fd: Int): Boolean
    private external fun nativeRefreshIsRunning(handle: Long): Boolean
    private external fun nativeRefreshLoopUntilSynced(handle: Long, skipCoinbase: Boolean): Int
    private external fun nativeRestoreAccount(
        handle: Long,
        secretScalar: ByteArray,
        accountTimestamp: Long
    )

    private external fun nativeSave(handle: Long, fd: Int): Boolean
    private external fun nativeSetRefreshSince(handle: Long, heightOrTimestamp: Long)
    private external fun nativeStopRefresh(handle: Long)
}
