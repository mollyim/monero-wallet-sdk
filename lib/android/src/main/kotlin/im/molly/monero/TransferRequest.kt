package im.molly.monero

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface TransferRequest : Parcelable

@Parcelize
data class PaymentRequest(
    val paymentDetails: List<PaymentDetail>,
    val spendingAccountIndex: Int,
    val feePriority: FeePriority? = null,
    val timeLock: UnlockTime? = null,
) : TransferRequest

@Parcelize
data class SweepRequest(
    val recipientAddress: PublicAddress,
    val splitCount: Int = 1,
    val keyImageHashes: List<HashDigest>,
    val feePriority: FeePriority? = null,
    val timeLock: UnlockTime? = null,
) : TransferRequest
