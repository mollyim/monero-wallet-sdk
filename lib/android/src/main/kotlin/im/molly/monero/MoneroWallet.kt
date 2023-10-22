package im.molly.monero

import im.molly.monero.internal.TxInfo
import im.molly.monero.internal.consolidateTransactions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant

class MoneroWallet internal constructor(
    private val wallet: IWallet,
    private val storageAdapter: StorageAdapter,
    val remoteNodeClient: RemoteNodeClient?,
) : AutoCloseable {

    private val logger = loggerFor<MoneroWallet>()

    val primaryAddress: String = wallet.accountPrimaryAddress

    var dataStore by storageAdapter::dataStore

    /**
     * A [Flow] of ledger changes.
     */
    fun ledger(): Flow<Ledger> = callbackFlow {
        val listener = object : IBalanceListener.Stub() {
            lateinit var lastKnownLedger: Ledger

            override fun onBalanceChanged(txHistory: List<TxInfo>, blockchainHeight: Int) {
                val now = Instant.now()
                val checkedAt = BlockchainTime(blockchainHeight, now)
                val (txs, spendableEnotes) = txHistory.consolidateTransactions(checkedAt)
                lastKnownLedger = Ledger(primaryAddress, txs, spendableEnotes, checkedAt)
                sendLedger(lastKnownLedger)
            }

            override fun onRefresh(blockHeight: Int) {
                val checkedAt = BlockchainTime.Block(blockHeight)
                sendLedger(lastKnownLedger.copy(checkedAt = checkedAt))
            }

            private fun sendLedger(ledger: Ledger) {
                trySend(ledger).onFailure {
                    logger.e("Too many ledger updates, channel capacity exceeded", it)
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
            override fun onRefreshResult(blockHeight: Int, status: Int) {
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
    override fun onRefreshResult(blockHeight: Int, status: Int) = Unit

    override fun onCommitResult(success: Boolean) = Unit
}

class RefreshResult(val blockHeight: Int, private val status: Int) {
    fun isError() = status != WalletNative.Status.OK
}
