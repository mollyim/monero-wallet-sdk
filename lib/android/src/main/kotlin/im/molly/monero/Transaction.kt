package im.molly.monero

data class Transaction(
    val hash: HashDigest,
    val state: TxState,
    val network: MoneroNetwork,
    val timeLock: UnlockTime?,
    val sent: Set<Enote>,
    val received: Set<Enote>,
    val payments: List<PaymentDetail>,
    val fee: MoneroAmount,
    val change: MoneroAmount,
) {
    val amount: MoneroAmount = received.sumOf { it.amount } - sent.sumOf { it.amount }

    val txId: String = hash.toString()

    private val blockHeader = (state as? TxState.OnChain)?.blockHeader

    val blockHeight = blockHeader?.height

    val blockTimestamp = blockHeader?.timestamp
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
