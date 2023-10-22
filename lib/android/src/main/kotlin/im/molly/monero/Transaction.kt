package im.molly.monero

import java.time.Instant

data class Transaction(
    val hash: HashDigest,
    // TODO: val version: ProtocolInfo,
    val state: TxState,
    val timeLock: BlockchainTime?,
    val sent: Set<Enote>,
    val received: Set<Enote>,
    val payments: List<PaymentDetail>,
    val fee: MoneroAmount,
    val change: MoneroAmount,
) {
    val amount: MoneroAmount = received.sumOf { it.amount } - sent.sumOf { it.amount }

    val txId: String
        get() = hash.toString()

    val blockHeight: Int?
        get() = (state as? TxState.OnChain)?.blockHeader?.height

    val timestamp: Instant?
        get() = (state as? TxState.OnChain)?.let { Instant.ofEpochSecond(it.blockHeader.epochSecond) }
}

sealed interface TxState {
    data class OnChain(
        val blockHeader: BlockHeader,
    ) : TxState

    data object BeingProcessed : TxState

    data object InMemoryPool : TxState

    data object Failed : TxState

    data object OffChain : TxState
}

data class PaymentDetail(
    val amount: MoneroAmount,
    val recipient: PublicAddress,
)
