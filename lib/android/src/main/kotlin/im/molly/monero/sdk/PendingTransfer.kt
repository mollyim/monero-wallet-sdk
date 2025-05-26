package im.molly.monero.sdk

import im.molly.monero.sdk.internal.IPendingTransfer
import im.molly.monero.sdk.internal.ITransferCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
class PendingTransfer internal constructor(
    private val pendingTransfer: IPendingTransfer,
) : AutoCloseable {

    val fee: MoneroAmount
        get() = pendingTransfer.fee.toAtomicUnits()

    val amount: MoneroAmount
        get() = pendingTransfer.amount.toAtomicUnits()

    val txCount: Int
        get() = pendingTransfer.txCount

    suspend fun commit(): Boolean = suspendCancellableCoroutine { continuation ->
        val callback = object : ITransferCallback.Stub() {
            override fun onTransferCreated(pendingTransfer: IPendingTransfer) = Unit

            override fun onTransferCommitted() {
                continuation.resume(true) {}
            }

            override fun onUnexpectedError(message: String) {
                continuation.resumeWithException(
                    IllegalStateException(message)
                )
            }
        }
        pendingTransfer.commitAndClose(callback)
    }

    override fun close() = pendingTransfer.close()
}
