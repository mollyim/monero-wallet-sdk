package im.molly.monero

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentDetail(
    val amount: MoneroAmount,
    val recipientAddress: PublicAddress,
) : Parcelable {
    init {
        require(amount >= 0) { "Payment amount cannot be negative" }
    }
}
