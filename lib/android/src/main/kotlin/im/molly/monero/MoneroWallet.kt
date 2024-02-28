package im.molly.monero

import im.molly.monero.internal.TxInfo
import im.molly.monero.internal.consolidateTransactions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MoneroWallet internal constructor(
    private val wallet: IWallet,
    private val storageAdapter: StorageAdapter,
    val remoteNodeClient: RemoteNodeClient?,
) : AutoCloseable {

    private val logger = loggerFor<MoneroWallet>()

    val publicAddress: PublicAddress = PublicAddress.parse(wallet.publicAddress)

    var dataStore by storageAdapter::dataStore

    suspend fun addDetachedSubAddress(accountIndex: Int, subAddressIndex: Int): AccountAddress =
        suspendCancellableCoroutine { continuation ->
            wallet.addDetachedSubAddress(
                accountIndex,
                subAddressIndex,
                object : BaseWalletCallbacks() {
                    override fun onAddressReady(subAddresses: Array<String>) {
                        val accountAddress = AccountAddress.parseWithIndexes(subAddresses[0])
                        continuation.resume(accountAddress) {}
                    }
                })
        }

    suspend fun createAccount(): AccountAddress =
        suspendCancellableCoroutine { continuation ->
            wallet.createAccount(object : BaseWalletCallbacks() {
                override fun onAddressReady(subAddresses: Array<String>) {
                    val accountAddress = AccountAddress.parseWithIndexes(subAddresses[0])
                    continuation.resume(accountAddress) {}
                }
            })
        }

    suspend fun createSubAddressForAccount(accountIndex: Int = 0): AccountAddress =
        suspendCancellableCoroutine { continuation ->
            wallet.createSubAddressForAccount(accountIndex, object : BaseWalletCallbacks() {
                override fun onAddressReady(subAddresses: Array<String>) {
                    if (subAddresses.isEmpty()) {
                        throw NoSuchAccountException(accountIndex)
                    }
                    val accountAddress = AccountAddress.parseWithIndexes(subAddresses[0])
                    continuation.resume(accountAddress) {}
                }
            })
        }

    suspend fun getAllAddresses(): Set<AccountAddress> =
        suspendCancellableCoroutine { continuation ->
            wallet.getAllAddresses(object : BaseWalletCallbacks() {
                override fun onAddressReady(subAddresses: Array<String>) {
                    continuation.resume(subAddresses.toAccountAddresses()) {}
                }
            })
        }

    private fun Array<String>.toAccountAddresses(): Set<AccountAddress> {
        return map { AccountAddress.parseWithIndexes(it) }.toSet()
    }

    /**
     * A [Flow] of ledger changes.
     */
    fun ledger(): Flow<Ledger> = callbackFlow {
        val listener = object : IBalanceListener.Stub() {
            lateinit var lastKnownLedger: Ledger

            override fun onBalanceChanged(
                txHistory: MutableList<TxInfo>,
                subAddresses: Array<String>,
                blockchainTime: BlockchainTime,
            ) {
                val accountAddresses = subAddresses.toAccountAddresses()
                val (txById, enotes) = txHistory.consolidateTransactions(
                    accountAddresses = accountAddresses,
                    blockchainContext = blockchainTime,
                )
                val ledger = Ledger(
                    publicAddress = publicAddress,
                    accountAddresses = accountAddresses,
                    transactionById = txById,
                    enotes = enotes,
                    checkedAt = blockchainTime,
                )
                sendLedger(ledger)
            }

            override fun onRefresh(blockchainTime: BlockchainTime) {
                sendLedger(lastKnownLedger.copy(checkedAt = blockchainTime))
            }

            override fun onAddressCreated(subAddress: String) {
                val addressSet = lastKnownLedger.accountAddresses.toMutableSet()
                val accountAddress = AccountAddress.parseWithIndexes(subAddress)
                if (addressSet.add(accountAddress)) {
                    sendLedger(lastKnownLedger.copy(accountAddresses = addressSet))
                }
            }

            private fun sendLedger(ledger: Ledger) {
                lastKnownLedger = ledger
                // Shouldn't block as we conflate the flow.
                trySendBlocking(ledger)
            }
        }

        wallet.addBalanceListener(listener)

        awaitClose { wallet.removeBalanceListener(listener) }
    }.conflate()

    suspend fun awaitRefresh(
        ignoreMiningRewards: Boolean = true,
    ): RefreshResult = suspendCancellableCoroutine { continuation ->
        wallet.resumeRefresh(ignoreMiningRewards, object : BaseWalletCallbacks() {
            override fun onRefreshResult(blockchainTime: BlockchainTime, status: Int) {
                val result = RefreshResult(blockchainTime, status)
                continuation.resume(result) {}
            }
        })

        continuation.invokeOnCancellation { wallet.cancelRefresh() }
    }

    suspend fun commit(): Boolean = suspendCancellableCoroutine { continuation ->
        wallet.commit(object : BaseWalletCallbacks() {
            override fun onCommitResult(success: Boolean) {
                continuation.resume(success) {}
            }
        })
    }

    suspend fun createTransfer(transferRequest: TransferRequest): PendingTransfer =
        suspendCancellableCoroutine { continuation ->
            val callback = object : ITransferRequestCallback.Stub() {
                override fun onTransferCreated(pendingTransfer: IPendingTransfer) {
                    continuation.resume(PendingTransfer(pendingTransfer)) {
                        pendingTransfer.close()
                    }
                }
            }
            when (transferRequest) {
                is PaymentRequest -> wallet.createPayment(transferRequest, callback)
                is SweepRequest -> wallet.createSweep(transferRequest, callback)
            }
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

class NoSuchAccountException(private val accountIndex: Int) : NoSuchElementException() {
    override val message: String
        get() = "No account was found with the specified index: $accountIndex"
}

private abstract class BaseWalletCallbacks : IWalletCallbacks.Stub() {
    override fun onAddressReady(subAddresses: Array<String>) = Unit

    override fun onRefreshResult(blockchainTime: BlockchainTime, status: Int) = Unit

    override fun onCommitResult(success: Boolean) = Unit

    override fun onFeesReceived(fees: LongArray?) = Unit
}

class RefreshResult(val blockchainTime: BlockchainTime, private val status: Int) {
    fun isError() = status != WalletNative.Status.OK
}
