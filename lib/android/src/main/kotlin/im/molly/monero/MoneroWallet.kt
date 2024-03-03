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

//    suspend fun addDetachedSubAddress(accountIndex: Int, subAddressIndex: Int): AccountAddress =
//        suspendCancellableCoroutine { continuation ->
//            wallet.addDetachedSubAddress(
//                accountIndex,
//                subAddressIndex,
//                object : BaseWalletCallbacks() {
//                    override fun onSubAddressReady(subAddress: String) {
//                        continuation.resume(AccountAddress.parseWithIndexes(subAddress)) {}
//                    }
//                })
//        }

    suspend fun createAccount(): WalletAccount =
        suspendCancellableCoroutine { continuation ->
            wallet.createAccount(object : BaseWalletCallbacks() {
                override fun onSubAddressReady(subAddress: String) {
                    val primaryAddress = AccountAddress.parseWithIndexes(subAddress)
                    continuation.resume(
                        WalletAccount(
                            addresses = listOf(primaryAddress),
                            accountIndex = primaryAddress.accountIndex,
                        )
                    ) {}
                }
            })
        }

    /**
     * @throws NoSuchAccountException
     */
    suspend fun createSubAddressForAccount(accountIndex: Int = 0): AccountAddress =
        suspendCancellableCoroutine { continuation ->
            wallet.createSubAddressForAccount(accountIndex, object : BaseWalletCallbacks() {
                override fun onSubAddressReady(subAddress: String) {
                    continuation.resume(AccountAddress.parseWithIndexes(subAddress)) {}
                }
            })
        }

    /**
     * @throws NoSuchAccountException
     */
    suspend fun findUnusedSubAddress(accountIndex: Int = 0): AccountAddress? {
        val ledger = ledger().first()
        val transactions = ledger.transactions
        val account = ledger.indexedAccounts.getOrNull(accountIndex)
            ?: throw NoSuchAccountException(accountIndex)

        return account.addresses.firstOrNull { !it.isAddressUsed(transactions) }
    }

    /**
     * @throws NoSuchAccountException
     */
    suspend fun getAccount(accountIndex: Int = 0): WalletAccount =
        suspendCancellableCoroutine { continuation ->
            wallet.getAddressesForAccount(accountIndex, object : BaseWalletCallbacks() {
                override fun onSubAddressListReceived(subAddresses: Array<String>) {
                    val accounts = parseAndAggregateAddresses(subAddresses)
                    continuation.resume(accounts.single()) {}
                }
            })
        }

    suspend fun getAllAccounts(): List<WalletAccount> =
        suspendCancellableCoroutine { continuation ->
            wallet.getAllAddresses(object : BaseWalletCallbacks() {
                override fun onSubAddressListReceived(subAddresses: Array<String>) {
                    val accounts = parseAndAggregateAddresses(subAddresses)
                    continuation.resume(accounts) {}
                }
            })
        }

    private fun parseAndAggregateAddresses(subAddresses: Array<String>): List<WalletAccount> =
        subAddresses.map { AccountAddress.parseWithIndexes(it) }
            .groupBy { it.accountIndex }
            .map { (index, addresses) ->
                WalletAccount(
                    addresses = addresses,
                    accountIndex = index,
                )
            }
            .sortedBy { it.accountIndex }

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
                val indexedAccounts = parseAndAggregateAddresses(subAddresses)
                val (txById, enotes) = txHistory.consolidateTransactions(
                    accounts = indexedAccounts,
                    blockchainContext = blockchainTime,
                )
                val ledger = Ledger(
                    publicAddress = publicAddress,
                    indexedAccounts = indexedAccounts,
                    transactionById = txById,
                    enoteSet = enotes,
                    checkedAt = blockchainTime,
                )
                sendLedger(ledger)
            }

            override fun onRefresh(blockchainTime: BlockchainTime) {
                sendLedger(lastKnownLedger.copy(checkedAt = blockchainTime))
            }

            override fun onSubAddressListUpdated(subAddresses: Array<String>) {
                val accountsUpdated = parseAndAggregateAddresses(subAddresses)
                if (lastKnownLedger.indexedAccounts != accountsUpdated) {
                    sendLedger(lastKnownLedger.copy(indexedAccounts = accountsUpdated))
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
                1 -> mapOf(FeePriority.Medium to fees[0])
                4 -> mapOf(
                    FeePriority.Low to fees[0],
                    FeePriority.Medium to fees[1],
                    FeePriority.High to fees[2],
                    FeePriority.Urgent to fees[3],
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
    override fun onRefreshResult(blockchainTime: BlockchainTime, status: Int) = Unit

    override fun onCommitResult(success: Boolean) = Unit

    override fun onSubAddressReady(subAddress: String) = Unit

    override fun onSubAddressListReceived(subAddresses: Array<String>) = Unit

    override fun onFeesReceived(fees: LongArray?) = Unit
}

class RefreshResult(val blockchainTime: BlockchainTime, private val status: Int) {
    fun isError() = status != WalletNative.Status.OK
}
