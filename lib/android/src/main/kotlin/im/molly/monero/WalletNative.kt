package im.molly.monero

import android.os.ParcelFileDescriptor
import androidx.annotation.GuardedBy
import im.molly.monero.internal.HttpRequest
import im.molly.monero.internal.HttpResponse
import im.molly.monero.internal.IHttpRequestCallback
import im.molly.monero.internal.IHttpRpcClient
import im.molly.monero.internal.TxInfo
import kotlinx.coroutines.*
import java.io.Closeable
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

internal class WalletNative private constructor(
    private val network: MoneroNetwork,
    private val storageAdapter: IStorageAdapter,
    private val rpcClient: IHttpRpcClient?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : IWallet.Stub(), Closeable {

    companion object {
        // TODO: Find better name because this is a local synchronization wallet, not a full node wallet
        suspend fun fullNode(
            networkId: Int,
            storageAdapter: IStorageAdapter,
            rpcClient: IHttpRpcClient? = null,
            secretSpendKey: SecretKey? = null,
            restorePoint: Long? = null,
            coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob(),
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = WalletNative(
            network = MoneroNetwork.fromId(networkId),
            storageAdapter = storageAdapter,
            rpcClient = rpcClient,
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

    private fun getSubAddresses(accountIndex: Int? = null): Array<String> {
        return nativeGetSubAddresses(accountIndex ?: -1, handle)
    }

    private fun getTxHistorySnapshot(): List<TxInfo> {
        return nativeGetTxHistory(handle).toList()
    }

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

    override fun createPayment(request: PaymentRequest, callback: ITransferCallback) {
        if (request.paymentDetails.any { it.recipientAddress.network != network }) {
            callback.onUnexpectedError("Recipient address is on a different network")
            return
        }
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
                accountIndex = request.spendingAccountIndex,
                subAddressIndexes = IntArray(0),
                callback = callback,
            )
        }
    }

    override fun createSweep(request: SweepRequest, callback: ITransferCallback) {
        TODO()
    }

    @CalledByNative
    private fun createPendingTransfer(
        transferHandle: Long,
        amount: Long,
        fee: Long,
        txCount: Int,
    ): IPendingTransfer =
        NativePendingTransfer(transferHandle, amount, fee, txCount)

    inner class NativePendingTransfer(
        private val transferHandle: Long,
        private val amount: Long,
        private val fee: Long,
        private val txCount: Int,
    ) : Closeable,
        IPendingTransfer.Stub() {

        private val closed = AtomicBoolean()

        override fun getAmount() = amount

        override fun getFee() = fee

        override fun getTxCount() = txCount

        override fun commitAndClose(callback: ITransferCallback) {
            scope.launch(singleThreadedDispatcher) {
                if (closed.compareAndSet(false, true)) {
                    nativeCommitPendingTransfer(handle, transferHandle, callback)
                    nativeDispose(transferHandle)
                } else {
                    callback.onUnexpectedError("PendingTransfer is closed")
                }
            }
        }

        override fun close() {
            if (closed.getAndSet(true)) return
            nativeDispose(transferHandle)
        }

        protected fun finalize() {
            close()
        }

        private external fun nativeDispose(transferHandle: Long)
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
        val txHistory = getTxHistorySnapshot()
        val subAddresses = getSubAddresses()

        balanceListenersLock.withLock {
            balanceListeners.add(listener)
            listener.onBalanceChanged(txHistory, subAddresses, currentBlockchainTime)
        }
    }

    override fun removeBalanceListener(listener: IBalanceListener) {
        balanceListenersLock.withLock {
            balanceListeners.remove(listener)
        }
    }

    override fun addDetachedSubAddress(
        accountIndex: Int,
        subAddressIndex: Int,
        callback: IWalletCallbacks?,
    ) {
        scope.launch(ioDispatcher) {
            val subAddress = nativeAddDetachedSubAddress(handle, accountIndex, subAddressIndex)
            notifyAddressCreation(subAddress, callback)
        }
    }

    override fun createAccount(callback: IWalletCallbacks?) {
        scope.launch(ioDispatcher) {
            val primaryAddress = nativeCreateSubAddressAccount(handle)
            notifyAddressCreation(primaryAddress, callback)
        }
    }

    override fun createSubAddressForAccount(accountIndex: Int, callback: IWalletCallbacks?) {
        scope.launch(ioDispatcher) {
            val subAddress = nativeCreateSubAddress(handle, accountIndex)
            if (subAddress != null) {
                notifyAddressCreation(subAddress, callback)
            } else {
                TODO()
            }
        }
    }

    private fun notifyAddressCreation(subAddress: String, callback: IWalletCallbacks?) {
        balanceListenersLock.withLock {
            if (balanceListeners.isNotEmpty()) {
                val subAddresses = getSubAddresses()
                balanceListeners.forEach { listener ->
                    listener.onSubAddressListUpdated(subAddresses)
                }
            }
        }
        callback?.onSubAddressReady(subAddress)
    }

    override fun getAddressesForAccount(accountIndex: Int, callback: IWalletCallbacks) {
        scope.launch(ioDispatcher) {
            val accountSubAddresses = getSubAddresses(accountIndex)
            if (accountSubAddresses.isNotEmpty()) {
                callback.onSubAddressListReceived(accountSubAddresses)
            } else {
                TODO()
            }
        }
    }

    override fun getAllAddresses(callback: IWalletCallbacks) {
        scope.launch(ioDispatcher) {
            callback.onSubAddressListReceived(getSubAddresses())
        }
    }

    @CalledByNative
    private fun onRefresh(height: Int, timestamp: Long, balanceChanged: Boolean) {
        balanceListenersLock.withLock {
            if (balanceListeners.isNotEmpty()) {
                val blockchainTime = network.blockchainTime(height, timestamp)
                val call = if (balanceChanged) {
                    val txHistory = getTxHistorySnapshot()
                    val subAddresses = getSubAddresses()
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
        method: String,
        path: String,
        header: String?,
        body: ByteArray?,
    ): HttpResponse? = runBlocking {
        pendingRequestLock.withLock {
            if (!requestsAllowed) {
                return@runBlocking null
            }
            val httpRequest = HttpRequest(method, path, header, body)
            pendingRequest = async {
                rpcClient?.newCall(httpRequest)
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

    private val callCounter = AtomicInteger()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun IHttpRpcClient.newCall(request: HttpRequest): HttpResponse? =
        suspendCancellableCoroutine { continuation ->
            val callback = object : IHttpRequestCallback.Stub() {
                override fun onResponse(response: HttpResponse) {
                    continuation.resume(response) {
                        response.close()
                    }
                }

                override fun onError() {
                    continuation.resume(null) {}
                }

                override fun onRequestCanceled() {
                    continuation.resume(null) {}
                }
            }
            val callId = callCounter.incrementAndGet()
            callAsync(request, callback, callId)
            continuation.invokeOnCancellation {
                cancelAsync(callId)
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

    private external fun nativeAddDetachedSubAddress(
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
        callback: ITransferCallback,
    )

    private external fun nativeCommitPendingTransfer(
        handle: Long,
        transferHandle: Long,
        callback: ITransferCallback,
    )

    private external fun nativeCreateSubAddressAccount(handle: Long): String
    private external fun nativeCreateSubAddress(handle: Long, subAddressMajor: Int): String?
    private external fun nativeDispose(handle: Long)
    private external fun nativeGetPublicAddress(handle: Long): String
    private external fun nativeGetCurrentBlockchainHeight(handle: Long): Int
    private external fun nativeGetCurrentBlockchainTimestamp(handle: Long): Long
    private external fun nativeGetSubAddresses(
        subAddressMajor: Int,
        handle: Long,
    ): Array<String>

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
