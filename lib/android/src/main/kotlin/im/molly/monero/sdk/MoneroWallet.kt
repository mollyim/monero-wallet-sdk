package im.molly.monero.sdk

import im.molly.monero.sdk.exceptions.InternalRuntimeException
import im.molly.monero.sdk.exceptions.NoSuchAccountException
import im.molly.monero.sdk.internal.DataStoreAdapter
import im.molly.monero.sdk.internal.IBalanceListener
import im.molly.monero.sdk.internal.IPendingTransfer
import im.molly.monero.sdk.internal.ITransferCallback
import im.molly.monero.sdk.internal.IWallet
import im.molly.monero.sdk.internal.IWalletCallbacks
import im.molly.monero.sdk.internal.LedgerFactory
import im.molly.monero.sdk.internal.NativeWallet
import im.molly.monero.sdk.internal.TxInfo
import im.molly.monero.sdk.internal.loggerFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MoneroWallet internal constructor(
    private val wallet: IWallet,
    private val defaultStore: DataStoreAdapter?,
    val moneroNodeClient: MoneroNodeClient?,
) : AutoCloseable {

    val publicAddress: PublicAddress = PublicAddress.parse(wallet.publicAddress)

    val network: MoneroNetwork
        get() = publicAddress.network

    private val logger = loggerFor<MoneroWallet>()

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

                override fun onAccountNotFound(accountIndex: Int) {
                    continuation.resumeWithException(NoSuchAccountException(accountIndex))
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
                    val accounts = parseAndAggregateAddresses(subAddresses.asIterable())
                    continuation.resume(accounts.single()) {}
                }

                override fun onAccountNotFound(accountIndex: Int) {
                    continuation.resumeWithException(NoSuchAccountException(accountIndex))
                }
            })
        }

    suspend fun getAllAccounts(): List<WalletAccount> =
        suspendCancellableCoroutine { continuation ->
            wallet.getAllAddresses(object : BaseWalletCallbacks() {
                override fun onSubAddressListReceived(subAddresses: Array<String>) {
                    val accounts = parseAndAggregateAddresses(subAddresses.asIterable())
                    continuation.resume(accounts) {}
                }
            })
        }

    suspend fun <R> withViewKey(block: suspend (SecretKey) -> R): R {
        return wallet.getViewSecretKey().use { viewKey ->
            block(viewKey)
        }
    }

    suspend fun <R> withSpendKey(block: suspend (SecretKey) -> R): R {
        return wallet.getSpendSecretKey().use { spendKey ->
            block(spendKey)
        }
    }

    suspend fun <R> withViewAndSpendKeys(block: suspend (viewKey: SecretKey, spendKey: SecretKey) -> R): R {
        return wallet.getViewSecretKey().use { viewKey ->
            wallet.getSpendSecretKey().use { spendKey ->
                block(viewKey, spendKey)
            }
        }
    }

    /**
     * A [Flow] of ledger changes.
     */
    fun ledger(): Flow<Ledger> = callbackFlow {
        val listener = object : IBalanceListener.Stub() {
            private lateinit var lastKnownLedger: Ledger

            private val txListBuffer = mutableListOf<TxInfo>()

            override fun onBalanceUpdateFinalized(
                txBatch: List<TxInfo>,
                allSubAddresses: Array<String>,
                blockchainTime: BlockchainTime,
            ) {
                val txList =
                    if (txListBuffer.isEmpty()) txBatch else txListBuffer.apply { addAll(txBatch) }

                val accounts = parseAndAggregateAddresses(allSubAddresses.asIterable())
                val ledger = LedgerFactory.createFromTxHistory(
                    txList = txList,
                    accounts = accounts,
                    blockchainTime = blockchainTime,
                )

                txListBuffer.clear()
                sendLedger(ledger)
            }

            override fun onBalanceUpdateChunk(txBatch: List<TxInfo>) {
                txListBuffer.addAll(txBatch)
            }

            override fun onWalletRefreshed(blockchainTime: BlockchainTime) {
                sendLedger(lastKnownLedger.copy(checkedAt = blockchainTime))
            }

            override fun onSubAddressListUpdated(allSubAddresses: Array<String>) {
                val accounts = parseAndAggregateAddresses(allSubAddresses.asIterable())
                if (accounts != lastKnownLedger.indexedAccounts) {
                    sendLedger(lastKnownLedger.copy(indexedAccounts = accounts))
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

    suspend fun save() =
        saveToDataStore(
            adapter = defaultStore ?: error("No dataStore associated with this wallet"),
            overwrite = true,
        )

    suspend fun save(targetStore: WalletDataStore, overwrite: Boolean = false) =
        saveToDataStore(
            adapter = DataStoreAdapter(targetStore),
            overwrite = overwrite,
        )

    private suspend fun saveToDataStore(adapter: DataStoreAdapter, overwrite: Boolean) {
        adapter.saveWithFd(overwrite) { fd ->
            suspendCoroutine { continuation ->
                val callback = object : BaseWalletCallbacks() {
                    override fun onCommitResult(success: Boolean) {
                        if (success) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                InternalRuntimeException(
                                    "Serialization error: Wallet data could not be saved"
                                )
                            )
                        }
                    }
                }
                wallet.commit(fd, callback)
            }
        }
    }

    suspend fun createTransfer(transferRequest: TransferRequest): PendingTransfer =
        suspendCancellableCoroutine { continuation ->
            val callback = object : ITransferCallback.Stub() {
                override fun onTransferCreated(pendingTransfer: IPendingTransfer) {
                    continuation.resume(PendingTransfer(pendingTransfer)) {
                        pendingTransfer.close()
                    }
                }

                override fun onTransferCommitted() = Unit

                override fun onUnexpectedError(message: String) {
                    continuation.resumeWithException(
                        IllegalStateException(message)
                    )
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

private abstract class BaseWalletCallbacks : IWalletCallbacks.Stub() {
    override fun onRefreshResult(blockchainTime: BlockchainTime, status: Int) = Unit

    override fun onCommitResult(success: Boolean) = Unit

    override fun onSubAddressReady(subAddress: String) = Unit

    override fun onSubAddressListReceived(subAddresses: Array<String>) = Unit

    override fun onAccountNotFound(accountIndex: Int) = Unit

    override fun onFeesReceived(fees: LongArray?) = Unit
}

class RefreshResult(val blockchainTime: BlockchainTime, private val status: Int) {
    fun isError() = status != NativeWallet.Status.OK
}
