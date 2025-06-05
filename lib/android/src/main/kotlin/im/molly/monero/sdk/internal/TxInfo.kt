package im.molly.monero.sdk.internal

import android.os.Parcelable
import im.molly.monero.sdk.BlockHeader
import im.molly.monero.sdk.BlockchainTime
import im.molly.monero.sdk.Enote
import im.molly.monero.sdk.EnoteOrigin
import im.molly.monero.sdk.HashDigest
import im.molly.monero.sdk.MoneroAmount
import im.molly.monero.sdk.PaymentDetail
import im.molly.monero.sdk.PublicAddress
import im.molly.monero.sdk.PublicKey
import im.molly.monero.sdk.TimeLocked
import im.molly.monero.sdk.Transaction
import im.molly.monero.sdk.TxState
import im.molly.monero.sdk.UnlockTime
import im.molly.monero.sdk.WalletAccount
import im.molly.monero.sdk.findAddressByIndex
import im.molly.monero.sdk.isBlockHeightInRange
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
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
    val txHash: @WriteWith<HexStringParceler> String,
    val publicKey: @WriteWith<HexStringParceler> String?,
    val keyImage: @WriteWith<HexStringParceler> String?,
    val subAddressMajor: Int,
    val subAddressMinor: Int,
    val recipient: String?,
    val amount: Long,
    val height: Int,
    val unlockTime: Long,
    val timestamp: Long,
    val fee: Long,
    val change: Long,
    val state: Byte,
    val coinbase: Boolean,
    val incoming: Boolean,
) : Parcelable {

    companion object {
        const val STATE_OFF_CHAIN: Byte = 1
        const val STATE_PENDING: Byte = 2
        const val STATE_FAILED: Byte = 3
        const val STATE_ON_CHAIN: Byte = 4

        const val MAX_PARCEL_SIZE_BYTES = 224
    }

    init {
        require(state in STATE_OFF_CHAIN..STATE_ON_CHAIN)
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
    val allEnotes = filter { it.incoming }
        .mapIndexed { txOutIndex, txInfo ->
            txInfo.toEnote(txOutIndex, blockchainContext.height, accounts)
        }

    val enoteByTxId = allEnotes.groupBy { enote ->
        enote.sourceTxId!!
    }

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

    val receivedEnotes = enoteByTxId.getOrDefault(txHash, emptyList())

    val outTxs = filter { !it.incoming }

    val spentKeyImages = outTxs.mapNotNull { it.keyImage }
    val sentEnotes = enoteByKeyImage.filterKeys { ki -> ki in spentKeyImages }.values

    val payments = outTxs.mapNotNull { it.toPaymentDetail() }

    return Transaction(
        hash = HashDigest(txHash),
        state = determineTxState(),
        network = blockchainContext.network,
        timeLock = timeLock,
        sent = sentEnotes.toSet(),
        received = receivedEnotes.toSet(),
        payments = payments,
        fee = MoneroAmount(atomicUnits = fee),
        change = MoneroAmount(atomicUnits = change),
    )
}

private fun List<TxInfo>.determineTxState(): TxState {
    return when (val state = first().state) {
        TxInfo.STATE_OFF_CHAIN -> TxState.OffChain
        TxInfo.STATE_PENDING -> TxState.InMemoryPool
        TxInfo.STATE_FAILED -> TxState.Failed
        TxInfo.STATE_ON_CHAIN -> {
            TxState.OnChain(
                BlockHeader(
                    height = maxOf { it.height },
                    epochSecond = maxOf { it.timestamp },
                )
            )
        }

        else -> error("Invalid tx state value: $state")
    }
}

private fun TxInfo.toEnote(
    txOutIndex: Int,
    blockchainHeight: Int,
    accounts: List<WalletAccount>,
): Enote {
    val ownerAddress = accounts.findAddressByIndex(subAddressMajor, subAddressMinor)
        ?: error("Failed to find subaddress: $subAddressMajor/$subAddressMinor")
    val calculatedAge = if (height > 0) (blockchainHeight - height + 1) else 0

    return Enote(
        amount = MoneroAmount(atomicUnits = amount),
        owner = ownerAddress,
        key = publicKey?.let { PublicKey(it) },
        keyImage = keyImage?.let { HashDigest(it) },
        age = calculatedAge,
        origin = EnoteOrigin.TxOut(txId = txHash, index = txOutIndex),
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
