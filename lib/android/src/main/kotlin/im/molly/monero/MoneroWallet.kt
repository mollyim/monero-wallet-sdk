package im.molly.monero

import im.molly.monero.internal.TxInfo
import im.molly.monero.internal.consolidateTransactions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.time.Duration.Companion.seconds

class MoneroWallet internal constructor(
    private val wallet: IWallet,
    private val storageAdapter: StorageAdapter,
    val remoteNodeClient: RemoteNodeClient?,
) : AutoCloseable {

    private val logger = loggerFor<MoneroWallet>()

    val primaryAddress: PublicAddress = PublicAddress.parse(wallet.accountPrimaryAddress)

    var dataStore by storageAdapter::dataStore

    /**
     * A [Flow] of ledger changes.
     */
    fun ledger(): Flow<Ledger> = callbackFlow {
        val listener = object : IBalanceListener.Stub() {
            lateinit var lastKnownLedger: Ledger

            override fun onBalanceChanged(txHistory: List<TxInfo>, blockchainTime: BlockchainTime) {
                val (txs, spendableEnotes) = txHistory.consolidateTransactions(blockchainTime)
                lastKnownLedger = Ledger(primaryAddress, txs, spendableEnotes, blockchainTime)
                sendLedger(lastKnownLedger)
            }

            override fun onRefresh(blockchainTime: BlockchainTime) {
                sendLedger(lastKnownLedger.copy(checkedAt = blockchainTime))
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
            override fun onRefreshResult(blockchainTime: BlockchainTime, status: Int) {
                val result = RefreshResult(blockchainTime, status)
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

    fun dynamicFeeRate(): Flow<DynamicFeeRate> = flow {
        while (true) {
            val fees = requestFees() ?: emptyList()
            val feePerByte = when (fees.size) {
                1 -> mapOf(FeePriority.MEDIUM to fees[0])
                4 -> mapOf(
                    FeePriority.LOW to fees[0],
                    FeePriority.MEDIUM to fees[1],
                    FeePriority.HIGH to fees[2],
                    FeePriority.URGENT to fees[3],
                )

                else -> {
                    logger.e("Unexpected number of fees received: ${fees.size}")
                    null
                }
            }
            feePerByte?.let { emit(DynamicFeeRate(it)) }
            // RPC client caches fees for 30 secs, wait before re-requesting fees
            delay(30.seconds)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun requestFees(): List<MoneroAmount>? =
        suspendCancellableCoroutine { continuation ->
            wallet.requestFees(object : BaseWalletCallbacks() {
                override fun onFeesReceived(fees: LongArray?) {
                    val feeAmounts = fees?.map { MoneroAmount(atomicUnits = it) }
                    continuation.resume(feeAmounts) {}
                }
            })
        }

    override fun close() = wallet.close()
}

private abstract class BaseWalletCallbacks : IWalletCallbacks.Stub() {
    override fun onRefreshResult(blockchainTime: BlockchainTime, status: Int) = Unit

    override fun onCommitResult(success: Boolean) = Unit

    override fun onFeesReceived(fees: LongArray?) = Unit
}

class RefreshResult(val blockchainTime: BlockchainTime, private val status: Int) {
    fun isError() = status != WalletNative.Status.OK
}
