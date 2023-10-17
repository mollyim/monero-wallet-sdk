package im.molly.monero

data class Transaction(
    val hash: HashDigest,
    // TODO: val version: ProtocolInfo,
    val state: TxState,
    val timeLock: BlockchainTime,
    val sent: Set<Enote>,
    val received: Set<Enote>,
    val payments: List<PaymentDetail>,
    val fee: MoneroAmount,
    val change: MoneroAmount,
) {
    val txId: String get() = hash.toString()
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
