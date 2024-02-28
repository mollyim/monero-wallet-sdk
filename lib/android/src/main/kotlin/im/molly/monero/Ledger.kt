package im.molly.monero

//import im.molly.monero.proto.LedgerProto

data class Ledger(
    val publicAddress: PublicAddress,
    val accountAddresses: Set<AccountAddress>,
    val transactionById: Map<String, Transaction>,
    val enotes: Set<TimeLocked<Enote>>,
    val checkedAt: BlockchainTime,
) {
    val transactions get() = transactionById.values

    val balance: Balance = enotes.calculateBalance()

//    companion object {
//        fun fromProto(proto: LedgerProto) = Ledger(
//            publicAddress = PublicAddress.base58(proto.publicAddress),
//            txOuts = proto.ownedTxOutsList.map { OwnedTxOut.fromProto(it) },
//            checkedAtBlockHeight = proto.blockHeight,
//        )
//    }
//
//    fun proto(): LedgerProto = LedgerProto.newBuilder()
//        .setPublicAddress(publicAddress.base58)
//        .addAllOwnedTxOuts(txOuts.map { it.proto() })
//        .setBlockHeight(checkedAtBlockHeight)
//        .build()
}
