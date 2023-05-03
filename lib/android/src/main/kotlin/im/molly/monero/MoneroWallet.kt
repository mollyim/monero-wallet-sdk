package im.molly.monero

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class MoneroWallet internal constructor(
    private val wallet: IWallet,
    private val storageAdapter: StorageAdapter,
    val remoteNodeClient: RemoteNodeClient?,
) : AutoCloseable {

    private val logger = loggerFor<MoneroWallet>()

    val primaryAddress: String = wallet.primaryAccountAddress

    var dataStore by storageAdapter::dataStore

    /**
     * A [Flow] of ledger changes.
     */
    fun ledger(): Flow<Ledger> = callbackFlow {
        val listener = object : IBalanceListener.Stub() {
            lateinit var lastKnownLedger: Ledger

            override fun onBalanceChanged(txOuts: List<OwnedTxOut>?, checkedAtBlockHeight: Long) {
                lastKnownLedger = Ledger(primaryAddress, txOuts!!, checkedAtBlockHeight)
                sendLedger(lastKnownLedger)
            }

            override fun onRefresh(blockchainHeight: Long) {
                sendLedger(lastKnownLedger.copy(checkedAtBlockHeight = blockchainHeight))
            }

            private fun sendLedger(ledger: Ledger) {
                trySend(ledger).onFailure {
                    logger.e("Too many ledger updates, channel capacity exceeded")
                }
            }
        }

        wallet.addBalanceListener(listener)

        awaitClose { wallet.removeBalanceListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun awaitRefresh(
        skipCoinbaseOutputs: Boolean = false,
    ): RefreshResult = suspendCancellableCoroutine { continuation ->
        wallet.resumeRefresh(skipCoinbaseOutputs, object : BaseWalletCallbacks() {
            override fun onRefreshResult(blockHeight: Long, status: Int) {
                val result = RefreshResult(blockHeight, status)
                continuation.resume(result) {}
            }
        })

        continuation.invokeOnCancellation { wallet.cancelRefresh() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun commit(): Boolean = suspendCancellableCoroutine { continuation ->
        wallet.commit(object : BaseWalletCallbacks() {
            override fun onCommitResult(success: Boolean) {
                continuation.resume(success) {}
            }
        })
    }

    override fun close() = wallet.close()
}

private abstract class BaseWalletCallbacks : IWalletCallbacks.Stub() {
    override fun onRefreshResult(blockHeight: Long, status: Int) = Unit

    override fun onCommitResult(success: Boolean) = Unit
}

class RefreshResult(val blockHeight: Long, private val status: Int) {
    fun isError() = status != WalletNative.Status.OK
}
