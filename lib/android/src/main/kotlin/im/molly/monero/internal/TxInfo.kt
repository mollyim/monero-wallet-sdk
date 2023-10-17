package im.molly.monero.internal

import android.os.Parcelable
import im.molly.monero.AccountAddress
import im.molly.monero.MoneroAmount
import im.molly.monero.BlockHeader
import im.molly.monero.BlockchainTime
import im.molly.monero.CalledByNative
import im.molly.monero.Enote
import im.molly.monero.HashDigest
import im.molly.monero.PaymentDetail
import im.molly.monero.PublicAddress
import im.molly.monero.PublicKey
import im.molly.monero.TimeLocked
import im.molly.monero.Transaction
import im.molly.monero.TxState
import kotlinx.parcelize.Parcelize

/**
 * TxInfo represents transaction information in a compact and easily serializable format.
 *
 * When grouping multiple `TxInfo` objects into a list, you can parse them into more structured
 * objects such as [Block], [Transaction], and [Enote] to facilitate further processing of
 * transaction history data.
 */
@Parcelize
internal data class TxInfo
@CalledByNative("wallet.cc") constructor(
    val txHash: String,
    val key: String,
    val keyImage: String?,
    val subAddressMajor: Int,
    val subAddressMinor: Int,
    val recipient: String?,
    val amount: Long,
    val height: Int,
    val state: Int,
    val unlockTime: Long,
    val timestamp: Long,
    val fee: Long,
    val change: Long,
    val coinbase: Boolean,
    val incoming: Boolean,
) : Parcelable {

    companion object State {
        const val OFF_CHAIN: Int = 0
        const val PENDING: Int = 1
        const val FAILED: Int = 2
        const val ON_CHAIN: Int = 3
    }

    init {
        require(state in OFF_CHAIN..ON_CHAIN)
        require(amount >= 0 && fee >= 0 && change >= 0) {
            "TX amounts cannot be negative"
        }
    }
}

internal fun List<TxInfo>.consolidateTransactions(
    blockchainContext: BlockchainTime,
): Pair<Map<String, Transaction>, Set<TimeLocked<Enote>>> {
    val (enoteByKey, enoteByKeyImage) = extractEnotesFromIncomingTxs(blockchainContext)

    val timeLockedEnotes = HashSet<TimeLocked<Enote>>(enoteByKey.size)

    // Group transactions by their hash and then map each group to a Transaction
    val groupedByTxId = groupBy { it.txHash }
    val txById = groupedByTxId.mapValues { (_, infoList) ->
        createTransaction(blockchainContext, infoList, enoteByKey, enoteByKeyImage)
            .also { tx ->
                if (tx.state !is TxState.Failed) {
                    val lockedEnotesToAdd =
                        tx.received.map { enote -> TimeLocked(enote, tx.timeLock) }
                    timeLockedEnotes.addAll(lockedEnotesToAdd)
                    tx.sent.forEach { enote -> enote.spent = true }
                }
            }
    }

    return txById to timeLockedEnotes
}

private fun List<TxInfo>.extractEnotesFromIncomingTxs(
    blockchainContext: BlockchainTime,
): Pair<Map<String, Enote>, Map<String, Enote>> {
    val enoteByKey = mutableMapOf<String, Enote>()
    val enoteByKeyImage = mutableMapOf<String, Enote>()

    for (txInfo in filter { it.incoming }) {
        enoteByKey.computeIfAbsent(txInfo.key) {
            val enote = txInfo.toEnote(blockchainContext.height)
            txInfo.keyImage?.let { keyImage ->
                enoteByKeyImage[keyImage] = enote
            }
            enote
        }
    }

    return enoteByKey to enoteByKeyImage
}

private fun createTransaction(
    blockchainContext: BlockchainTime,
    infoList: List<TxInfo>,
    enoteMap: Map<String, Enote>,
    keyImageMap: Map<String, Enote>,
): Transaction {
    val txHash = infoList.first().txHash
    val unlockTime = infoList.maxOf { it.unlockTime }
    val fee = infoList.maxOf { it.fee }
    val change = infoList.maxOf { it.change }

    val (ins, outs) = infoList.partition { it.incoming }

    val receivedEnotes = ins.map { enoteMap.getValue(it.key) }
    val spentKeyImages = outs.mapNotNull { it.keyImage }.toSet()
    val sentEnotes = keyImageMap.filterKeys { spentKeyImages.contains(it) }.values
    val payments = outs.map { it.toPaymentDetail() }

    return Transaction(
        hash = HashDigest(txHash),
        state = determineTxState(infoList),
        timeLock = blockchainContext.resolveUnlockTime(unlockTime),
        sent = sentEnotes.toSet(),
        received = receivedEnotes.toSet(),
        payments = payments,
        fee = MoneroAmount(fee),
        change = MoneroAmount(change),
    )
}

private fun determineTxState(infoList: List<TxInfo>): TxState {
    val txInfo = infoList.distinctBy { it.state }.single()

    return when (txInfo.state) {
        TxInfo.OFF_CHAIN -> TxState.OffChain
        TxInfo.PENDING -> TxState.InMemoryPool
        TxInfo.FAILED -> TxState.Failed
        TxInfo.ON_CHAIN -> TxState.OnChain(BlockHeader(txInfo.height, txInfo.timestamp))
        else -> error("Invalid tx state value: ${txInfo.state}")
    }
}

private fun TxInfo.toEnote(blockchainHeight: Int): Enote {
    val ownerAddress = AccountAddress(
        publicAddress = PublicAddress.parse(recipient!!),
        accountIndex = subAddressMajor,
        subAddressIndex = subAddressMinor
    )

    val calculatedAge = if (height == 0) 0 else blockchainHeight - height + 1

    return Enote(
        amount = MoneroAmount(amount),
        owner = ownerAddress,
        key = PublicKey(key),
        keyImage = keyImage?.let { HashDigest(it) },
        age = calculatedAge,
    )
}

private fun TxInfo.toPaymentDetail() = PaymentDetail(
    amount = MoneroAmount(amount),
    recipient = PublicAddress.parse(recipient!!),
)
