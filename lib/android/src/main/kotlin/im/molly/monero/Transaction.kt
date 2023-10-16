package im.molly.monero

data class Transaction(
    val hash: HashDigest,
    // TODO: val version: ProtocolInfo,
    val state: TxState,
    val timeLock: BlockchainTime,
    val sent: Set<Enote>,
    val received: Set<Enote>,
    val payments: List<PaymentDetail>,
    val fee: AtomicAmount,
    val change: AtomicAmount,
) {
    val txId: String get() = hash.toString()

    val netAmount: AtomicAmount = calculateNetAmount()

    private fun calculateNetAmount(): AtomicAmount {
        val receivedSum = received.sumOf { it.amount }
        val sentSum = sent.sumOf { it.amount }
        return receivedSum - sentSum
    }
}

sealed interface TxState {
    val confirmed get() = this is OnChain

    data class OnChain(
        val blockHeader: BlockHeader,
    ) : TxState

    data object BeingProcessed : TxState

    data object InMemoryPool : TxState

    data object Failed : TxState

    data object OffChain : TxState
}

data class PaymentDetail(
    val amount: AtomicAmount,
    val recipient: PublicAddress,
)