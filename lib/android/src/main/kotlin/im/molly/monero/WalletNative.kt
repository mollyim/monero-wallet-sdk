package im.molly.monero

import android.os.ParcelFileDescriptor
import androidx.annotation.GuardedBy
import im.molly.monero.internal.TxInfo
import kotlinx.coroutines.*
import java.io.Closeable
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

internal class WalletNative private constructor(
    private val network: MoneroNetwork,
    private val storageAdapter: IStorageAdapter,
    private val remoteNodeClient: IRemoteNodeClient?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : IWallet.Stub(), Closeable {

    companion object {
        // TODO: Find better name because this is a local synchronization wallet, not a full node wallet
        suspend fun fullNode(
            networkId: Int,
            storageAdapter: IStorageAdapter,
            remoteNodeClient: IRemoteNodeClient? = null,
            secretSpendKey: SecretKey? = null,
            restorePoint: Long? = null,
            coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob(),
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = WalletNative(
            network = MoneroNetwork.fromId(networkId),
            storageAdapter = storageAdapter,
            remoteNodeClient = remoteNodeClient,
            scope = CoroutineScope(coroutineContext),
            ioDispatcher = ioDispatcher,
        ).apply {
            when {
                secretSpendKey != null -> {
                    require(restorePoint == null || restorePoint >= 0)
                    val restorePointOrNow = restorePoint ?: (System.currentTimeMillis() / 1000)
                    nativeRestoreAccount(handle, secretSpendKey.bytes, restorePointOrNow)
                    tryWriteState()
                }

                else -> {
                    require(restorePoint == null)
                    readState()
                }
            }
        }
    }

    private val logger = loggerFor<WalletNative>()

    init {
        NativeLoader.loadWalletLibrary(logger = logger)
    }

    private val handle: Long = nativeCreate(network.id)

    private suspend fun tryWriteState(): Boolean {
        return withContext(ioDispatcher) {
            val pipe = ParcelFileDescriptor.createPipe()
            val readFd = pipe[0]
            val writeFd = pipe[1]
            val storageIsReady = storageAdapter.writeAsync(readFd)
            if (storageAdapter.isRemote()) {
                readFd.close()
            }
            writeFd.use {
                if (storageIsReady) {
                    val result = nativeSave(handle, it.fd)
                    if (!result) {
                        logger.e("Wallet data serialization failed")
                    }
                    result
                } else {
                    logger.i("Unable to save wallet data because WalletDataStore is unset")
                    false
                }
            }
        }
    }

    private suspend fun readState() {
        withContext(ioDispatcher) {
            val pipe = ParcelFileDescriptor.createPipe()
            val readFd = pipe[0]
            val writeFd = pipe[1]
            storageAdapter.readAsync(writeFd)
            if (storageAdapter.isRemote()) {
                writeFd.close()
            }
            readFd.use {
                if (!nativeLoad(handle, it.fd)) {
                    error("Wallet data deserialization failed")
                }
            }
        }
    }

    override fun getPublicAddress() = nativeGetPublicAddress(handle)

    private fun MoneroNetwork.blockchainTime(height: Int, epochSecond: Long): BlockchainTime {
        // Block timestamp could be zero during a fast refresh.
        val timestamp = when (epochSecond) {
            0L -> estimateTimestamp(height)
            else -> Instant.ofEpochSecond(epochSecond)
        }
        return BlockchainTime(height = height, timestamp = timestamp, network = this)
    }

    val currentBlockchainTime: BlockchainTime
        get() = network.blockchainTime(
            nativeGetCurrentBlockchainHeight(handle),
            nativeGetCurrentBlockchainTimestamp(handle),
        )

    val currentBalance: Balance
        get() = TODO() // txHistorySnapshot().consolidateTransactions().second.balance()

    val subAddresses: Array<String>
        get() = nativeGetSubAddresses(handle)

    private fun txHistorySnapshot(): List<TxInfo> = nativeGetTxHistory(handle).toList()

    @GuardedBy("listenersLock")
    private val balanceListeners = mutableSetOf<IBalanceListener>()

    private val balanceListenersLock = ReentrantLock()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThreadedDispatcher = ioDispatcher.limitedParallelism(1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun resumeRefresh(
        skipCoinbase: Boolean,
        callback: IWalletCallbacks?,
    ) {
        scope.launch {
            val status = suspendCancellableCoroutine { continuation ->
                launch(singleThreadedDispatcher) {
                    continuation.resume(nativeNonReentrantRefresh(handle, skipCoinbase)) {}
                }
                continuation.invokeOnCancellation {
                    nativeCancelRefresh(handle)
                }
            }
            callback?.onRefreshResult(currentBlockchainTime, status)
        }
    }

    override fun cancelRefresh() {
        scope.launch(ioDispatcher) {
            nativeCancelRefresh(handle)
        }
    }

    override fun setRefreshSince(blockHeightOrTimestamp: Long) {
        scope.launch(ioDispatcher) {
            nativeSetRefreshSince(handle, blockHeightOrTimestamp)
        }
    }

    override fun commit(callback: IWalletCallbacks?) {
        scope.launch(ioDispatcher) {
            val result = tryWriteState()
            callback?.onCommitResult(result)
        }
    }

    override fun createPayment(request: PaymentRequest, callback: ITransferRequestCallback) {
        scope.launch(singleThreadedDispatcher) {
            val (amounts, addresses) = request.paymentDetails.map {
                it.amount.atomicUnits to it.recipientAddress.address
            }.unzip()

            nativeCreatePayment(
                handle = handle,
                addresses = addresses.toTypedArray(),
                amounts = amounts.toLongArray(),
                timeLock = request.timeLock?.blockchainTime?.toLong() ?: 0,
                priority = request.feePriority?.priority ?: 0,
                accountIndex = 0,
                subAddressIndexes = IntArray(0),
                callback = callback,
            )
        }
    }

    override fun createSweep(request: SweepRequest, callback: ITransferRequestCallback) {
        TODO()
    }

    @CalledByNative
    private fun createPendingTransfer(handle: Long) = NativePendingTransfer(handle)

    inner class NativePendingTransfer(private val handle: Long) : Closeable,
        IPendingTransfer.Stub() {

        private val closed = AtomicBoolean()

        override fun close() {
            if (closed.getAndSet(true)) return
            nativeDisposePendingTransfer(handle)
        }

        protected fun finalize() {
            nativeDisposePendingTransfer(handle)
        }
    }

    override fun requestFees(callback: IWalletCallbacks?) {
        scope.launch(ioDispatcher) {
            val fees = nativeFetchBaseFeeEstimate(handle)
            callback?.onFeesReceived(fees)
        }
    }

    /**
     * Also replays the last known balance whenever a new listener registers.
     */
    override fun addBalanceListener(listener: IBalanceListener) {
        balanceListenersLock.withLock {
            balanceListeners.add(listener)
            listener.onBalanceChanged(txHistorySnapshot(), subAddresses, currentBlockchainTime)
        }
    }

    override fun removeBalanceListener(listener: IBalanceListener) {
        balanceListenersLock.withLock {
            balanceListeners.remove(listener)
        }
    }

    override fun getOrCreateAddress(
        accountIndex: Int,
        subAddressIndex: Int,
        callback: IWalletCallbacks?,
    ) {
        scope.launch(ioDispatcher) {
            val subAddress = nativeAddSubAddress(handle, accountIndex, subAddressIndex)
            notifyAddressCreation(subAddress, callback)
        }
    }

    override fun createAccount(callback: IWalletCallbacks?) {
        scope.launch(ioDispatcher) {
            val subAddress = nativeCreateSubAddressAccount(handle)
            notifyAddressCreation(subAddress, callback)
        }
    }

    override fun createSubAddressForAccount(accountIndex: Int, callback: IWalletCallbacks?) {
        scope.launch(ioDispatcher) {
            val subAddress = nativeCreateSubAddress(handle, accountIndex)
            if (subAddress != null) {
                notifyAddressCreation(subAddress, callback)
            } else {
                callback?.onAddressReady(emptyArray())
            }
        }
    }

    private fun notifyAddressCreation(subAddress: String, callback: IWalletCallbacks?) {
        balanceListenersLock.withLock {
            balanceListeners.forEach { listener ->
                listener.onAddressCreated(subAddress)
            }
        }
        callback?.onAddressReady(arrayOf(subAddress))
    }

    override fun getAllAddresses(callback: IWalletCallbacks) {
        scope.launch(ioDispatcher) {
            callback.onAddressReady(subAddresses)
        }
    }

    @CalledByNative
    private fun onRefresh(height: Int, timestamp: Long, balanceChanged: Boolean) {
        balanceListenersLock.withLock {
            if (balanceListeners.isNotEmpty()) {
                val blockchainTime = network.blockchainTime(height, timestamp)
                val call = if (balanceChanged) {
                    val txHistory = txHistorySnapshot()
                    fun(listener: IBalanceListener) {
                        listener.onBalanceChanged(txHistory, subAddresses, blockchainTime)
                    }
                } else {
                    fun(listener: IBalanceListener) {
                        listener.onRefresh(blockchainTime)
                    }
                }
                balanceListeners.forEach { call(it) }
            }
        }
    }

    @CalledByNative
    private fun onSuspendRefresh(suspending: Boolean) {
        if (suspending) {
            pendingRequestLock.withLock {
                pendingRequest?.cancel()
                requestsAllowed = false
            }
        } else {
            requestsAllowed = true
        }
    }

    private var requestsAllowed = true

    @GuardedBy("pendingRequestLock")
    private var pendingRequest: Deferred<HttpResponse?>? = null

    private val pendingRequestLock = ReentrantLock()

    /**
     * Invoked by native code to make a cancellable remote call to a remote node.
     *
     * Caller must close [HttpResponse.body] upon completion of processing the response.
     */
    @CalledByNative
    private fun callRemoteNode(
        method: String?,
        path: String?,
        header: String?,
        body: ByteArray?,
    ): HttpResponse? = runBlocking {
        pendingRequestLock.withLock {
            if (!requestsAllowed) {
                return@runBlocking null
            }
            pendingRequest = async {
                remoteNodeClient?.request(HttpRequest(method, path, header, body))
            }
        }
        try {
            runCatching {
                pendingRequest?.await()
            }.onFailure { throwable ->
                if (throwable is CancellationException) {
                    return@onFailure
                }
                logger.e("Error waiting for HTTP response", throwable)
                throw throwable
            }.getOrNull()
        } finally {
            pendingRequest = null
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

    private external fun nativeAddSubAddress(
        handle: Long,
        subAddressMajor: Int,
        subAddressMinor: Int,
    ): String

    private external fun nativeCancelRefresh(handle: Long)
    private external fun nativeCreate(networkId: Int): Long
    private external fun nativeCreatePayment(
        handle: Long,
        addresses: Array<String>,
        amounts: LongArray,
        timeLock: Long,
        priority: Int,
        accountIndex: Int,
        subAddressIndexes: IntArray,
        callback: ITransferRequestCallback,
    )

    private external fun nativeCreateSubAddressAccount(handle: Long): String
    private external fun nativeCreateSubAddress(handle: Long, subAddressMajor: Int): String?
    private external fun nativeDispose(handle: Long)
    private external fun nativeDisposePendingTransfer(handle: Long)
    private external fun nativeGetPublicAddress(handle: Long): String
    private external fun nativeGetCurrentBlockchainHeight(handle: Long): Int
    private external fun nativeGetCurrentBlockchainTimestamp(handle: Long): Long
    private external fun nativeGetSubAddresses(handle: Long): Array<String>
    private external fun nativeGetTxHistory(handle: Long): Array<TxInfo>
    private external fun nativeFetchBaseFeeEstimate(handle: Long): LongArray
    private external fun nativeLoad(handle: Long, fd: Int): Boolean
    private external fun nativeNonReentrantRefresh(handle: Long, skipCoinbase: Boolean): Int
    private external fun nativeRestoreAccount(
        handle: Long, secretScalar: ByteArray, restorePoint: Long
    )

    private external fun nativeSave(handle: Long, fd: Int): Boolean
    private external fun nativeSetRefreshSince(handle: Long, heightOrTimestamp: Long)
}
