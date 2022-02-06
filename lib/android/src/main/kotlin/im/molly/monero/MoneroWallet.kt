package im.molly.monero

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class MoneroWallet(private val wallet: IWallet) : IWallet by wallet, AutoCloseable {

    val publicAddress: String = wallet.primaryAccountAddress

    /**
     * A [Flow] of ledger changes.
     */
    fun ledger(): Flow<Ledger> = callbackFlow {
        val listener = object : IBalanceListener.Stub() {
            lateinit var lastKnownLedger: Ledger

            override fun onBalanceChanged(txOuts: List<OwnedTxOut>?, checkedAtBlockHeight: Long) {
                lastKnownLedger = Ledger(publicAddress, txOuts!!, checkedAtBlockHeight)
                trySendBlocking(lastKnownLedger)
            }

            override fun onRefresh(blockchainHeight: Long) {
                trySendBlocking(lastKnownLedger.copy(checkedAtBlockHeight = blockchainHeight))
            }
        }

        addBalanceListener(listener)

        awaitClose { removeBalanceListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun awaitRefresh(
        skipCoinbaseOutputs: Boolean = false,
    ): RefreshResult = suspendCancellableCoroutine { continuation ->
        val callback = object : IRefreshCallback.Stub() {
            override fun onResult(blockHeight: Long, status: Int) {
                val result = RefreshResult(blockHeight, status)
                continuation.resume(result) {}
            }
        }

        restartRefresh(skipCoinbaseOutputs, callback)

        continuation.invokeOnCancellation { stopRefresh() }
    }
}

class RefreshResult(
    val blockHeight: Long,
    private val status: Int
) {
    fun isError() = status != WalletNative.Status.OK
}
