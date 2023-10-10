package im.molly.monero.internal

import android.os.Parcelable
import im.molly.monero.AccountAddress
import im.molly.monero.AtomicAmount
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

    val outgoing get() = !incoming

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
    blockchainTime: BlockchainTime,
): Pair<Map<String, Transaction>, Set<TimeLocked<Enote>>> {
    val enoteMap = mutableMapOf<String, Enote>()
    val keyImageMap = mutableMapOf<String, Enote>()
    val spentSet = mutableSetOf<String>()

    forEach { txInfo ->
        if (txInfo.incoming) {
            enoteMap.computeIfAbsent(txInfo.key) {
                txInfo.toEnote(blockchainTime.height).also { enote ->
                    txInfo.keyImage?.let { keyImageMap[it] = enote }
                }
            }
        } else if (txInfo.keyImage != null) {
            spentSet.add(txInfo.key)
        }
    }

    val groupedByTxHash = groupBy { it.txHash }
    val txs = groupedByTxHash.mapValues { (txHash, infoList) ->
        createTransaction(txHash, infoList, enoteMap, keyImageMap, blockchainTime)
    }

    val spendableEnotes = enoteMap
        .filterKeys { !spentSet.contains(it) }
        .map { (_, enote) ->
            TimeLocked(enote, txs[enote.emissionTxId]!!.timeLock)
        }
        .toSet()

    return txs to spendableEnotes
}

private fun createTransaction(
    txHash: String,
    infoList: List<TxInfo>,
    enoteMap: Map<String, Enote>,
    keyImageMap: Map<String, Enote>,
    blockchainTime: BlockchainTime,
): Transaction {
    val unlockTime = infoList.maxOf { it.unlockTime }
    val fee = infoList.maxOf { it.fee }
    val change = infoList.maxOf { it.change }

    val (ins, outs) = infoList.partition { it.incoming }
    val received = ins.map { enoteMap.getValue(it.key) }
    val spentKeyImages = outs.mapNotNull { it.keyImage }.toSet()
    val sent = keyImageMap.filterKeys { it in spentKeyImages }.values
    val payments = outs.map { it.toPaymentDetail() }

    return Transaction(
        hash = HashDigest(txHash),
        state = determineTxState(infoList),
        timeLock = blockchainTime.fromUnlockTime(unlockTime),
        sent = sent.toSet(),
        received = received.toSet(),
        payments = payments,
        fee = AtomicAmount(fee),
        change = AtomicAmount(change),
    )
}

private fun determineTxState(infoList: List<TxInfo>): TxState {
    val txInfo = infoList.distinctBy { it.state }.single()
    return when (txInfo.state) {
        TxInfo.OFF_CHAIN -> TxState.OffChain
        TxInfo.PENDING -> TxState.InMemoryPool
        TxInfo.FAILED -> TxState.Failed
        TxInfo.ON_CHAIN -> TxState.OnChain(BlockHeader(txInfo.height, txInfo.timestamp))
        else -> throw IllegalArgumentException("Invalid tx state value: ${txInfo.state}")
    }
}

private fun TxInfo.toEnote(blockchainHeight: Int) = Enote(
    amount = AtomicAmount(amount),
    owner = AccountAddress(
        publicAddress = PublicAddress.parse(recipient!!),
        accountIndex = subAddressMajor,
        subAddressIndex = subAddressMinor,
    ),
    key = PublicKey(key),
    keyImage = keyImage?.let { HashDigest(it) },
    emissionTxId = txHash,
    age = if (height == 0) 0 else (blockchainHeight - height + 1)
)

private fun TxInfo.toPaymentDetail() = PaymentDetail(
    amount = AtomicAmount(amount),
    recipient = PublicAddress.parse(recipient!!),
)
