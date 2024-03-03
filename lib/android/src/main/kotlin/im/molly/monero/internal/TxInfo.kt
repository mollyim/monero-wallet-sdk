package im.molly.monero.internal

import android.os.Parcelable
import im.molly.monero.BlockHeader
import im.molly.monero.BlockchainTime
import im.molly.monero.CalledByNative
import im.molly.monero.Enote
import im.molly.monero.HashDigest
import im.molly.monero.WalletAccount
import im.molly.monero.MoneroAmount
import im.molly.monero.PaymentDetail
import im.molly.monero.PublicAddress
import im.molly.monero.PublicKey
import im.molly.monero.TimeLocked
import im.molly.monero.Transaction
import im.molly.monero.TxState
import im.molly.monero.UnlockTime
import im.molly.monero.findAddressByIndex
import im.molly.monero.isBlockHeightInRange
import kotlinx.parcelize.Parcelize
import java.time.Instant

/**
 * TxInfo represents transaction information in a compact and easily serializable format.
 *
 * When grouping multiple `TxInfo` objects into a list, you can parse them into more structured
 * objects such as [Block], [Transaction], and [Enote] to facilitate further processing of
 * transaction history data.
 */
@Parcelize
internal data class TxInfo @CalledByNative constructor(
    val txHash: String,
    val publicKey: String?,
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
    accounts: List<WalletAccount>,
    blockchainContext: BlockchainTime,
): Pair<Map<String, Transaction>, Set<TimeLocked<Enote>>> {
    // Extract enotes from incoming transactions
    val allEnotes =
        filter { it.incoming }.map { it.toEnote(blockchainContext.height, accounts) }

    val enoteByTxId = allEnotes.groupBy { enote -> enote.sourceTxId!! }

    val enoteByKeyImage = allEnotes.mapNotNull { enote ->
        enote.keyImage?.let { keyImage -> keyImage.toString() to enote }
    }.toMap()

    val validEnotes = HashSet<TimeLocked<Enote>>(allEnotes.size)

    // Group transaction info by their hash and then map each group to a Transaction
    val groupedByTxId = groupBy { txInfo -> txInfo.txHash }
    val txById = groupedByTxId.mapValues { (_, infoList) ->
        val tx = infoList.createTransaction(blockchainContext, enoteByTxId, enoteByKeyImage)

        // If transaction isn't failed, calculate unlock time and save enotes
        if (tx.state !is TxState.Failed) {
            val unlockTime = tx.blockHeight?.let { height ->
                blockchainContext.effectiveUnlockTime(height, tx.timeLock)
            }
            val lockedEnotesToAdd = tx.received.map { enote ->
                TimeLocked(enote, unlockTime)
            }
            validEnotes.addAll(lockedEnotesToAdd)
        }

        // Mark the sent enotes as spent
        tx.sent.forEach { enote -> enote.markAsSpent() }

        tx
    }

    return txById to validEnotes
}

private fun List<TxInfo>.createTransaction(
    blockchainContext: BlockchainTime,
    enoteByTxId: Map<String, List<Enote>>,
    enoteByKeyImage: Map<String, Enote>,
): Transaction {
    val txHash = first().txHash

    val fee = maxOf { it.fee }
    val change = maxOf { it.change }

    val timeLock = maxOf { it.unlockTime }.let { unlockTime ->
        if (unlockTime == 0L) null
        else blockchainContext.resolveUnlockTime(unlockTime)
    }

    val receivedEnotes = enoteByTxId.getOrDefault(txHash, emptyList()).toSet()

    val outTxs = filter { !it.incoming }

    val spentKeyImages = outTxs.mapNotNull { it.keyImage }
    val sentEnotes = enoteByKeyImage.filterKeys { ki -> ki in spentKeyImages }.values.toSet()

    val payments = outTxs.mapNotNull { it.toPaymentDetail() }

    return Transaction(
        hash = HashDigest(txHash),
        state = determineTxState(),
        network = blockchainContext.network,
        timeLock = timeLock,
        sent = sentEnotes,
        received = receivedEnotes,
        payments = payments,
        fee = MoneroAmount(atomicUnits = fee),
        change = MoneroAmount(atomicUnits = change),
    )
}

private fun List<TxInfo>.determineTxState(): TxState {
    val height = maxOf { it.height }
    val timestamp = maxOf { it.timestamp }

    return when (val state = first().state) {
        TxInfo.OFF_CHAIN -> TxState.OffChain
        TxInfo.PENDING -> TxState.InMemoryPool
        TxInfo.FAILED -> TxState.Failed
        TxInfo.ON_CHAIN -> TxState.OnChain(BlockHeader(height, timestamp))
        else -> error("Invalid tx state value: $state")
    }
}

private fun TxInfo.toEnote(blockchainHeight: Int, accounts: List<WalletAccount>): Enote {
    val ownerAddress = accounts.findAddressByIndex(subAddressMajor, subAddressMinor)
        ?: error("Failed to find subaddress: $subAddressMajor/$subAddressMinor")
    val calculatedAge = if (height > 0) (blockchainHeight - height + 1) else 0

    return Enote(
        amount = MoneroAmount(atomicUnits = amount),
        owner = ownerAddress,
        key = publicKey?.let { PublicKey(it) },
        keyImage = keyImage?.let { HashDigest(it) },
        age = calculatedAge,
        sourceTxId = txHash,
    )
}

private fun TxInfo.toPaymentDetail(): PaymentDetail? {
    val recipientAddress = PublicAddress.parse(recipient ?: return null)
    return PaymentDetail(
        amount = MoneroAmount(atomicUnits = amount),
        recipientAddress = recipientAddress,
    )
}

private fun BlockchainTime.resolveUnlockTime(heightOrTimestamp: Long): UnlockTime {
    return if (isBlockHeightInRange(heightOrTimestamp)) {
        val height = heightOrTimestamp.toInt()
        UnlockTime.Block(
            BlockchainTime(height, estimateTimestamp(height), network)
        )
    } else {
        val clampedTs = if (heightOrTimestamp in network.epoch..Instant.MAX.epochSecond) {
            Instant.ofEpochSecond(heightOrTimestamp)
        } else Instant.MAX
        UnlockTime.Timestamp(
            BlockchainTime(estimateHeight(clampedTs), clampedTs, network)
        )
    }
}
