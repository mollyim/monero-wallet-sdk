package im.molly.monero

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//@Parcelize
data class PublicAddress(
    val network: MoneroNetwork,
//    // Address type
//    // viewPublicKey: ByteArray
//    // spendPublicKey: ByteArray
//    // Checksum
) {
    companion object {
        fun parse(publicAddress: String): PublicAddress {
            return PublicAddress(network = MoneroNetwork.Mainnet) // FIXME
        }
    }
}
