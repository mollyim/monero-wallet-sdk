package im.molly.monero

//import im.molly.monero.proto.LedgerProto

data class Ledger constructor(
    val publicAddress: String,
    val txOuts: List<OwnedTxOut>,
    val checkedAtBlockHeight: Long,
) {
    val balance = Balance.of(txOuts)

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
