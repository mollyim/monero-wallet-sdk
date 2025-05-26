package im.molly.monero.sdk

import im.molly.monero.sdk.internal.constants.PER_KB_FEE_QUANTIZATION_DECIMALS
import java.math.BigDecimal

data class DynamicFeeRate(val feePerByte: Map<FeePriority, MoneroAmount>) {

    val quantizationMask: MoneroAmount = BigDecimal.TEN.pow(PER_KB_FEE_QUANTIZATION_DECIMALS).xmr

    fun estimateFee(pendingTransfer: PendingTransfer): Map<FeePriority, MoneroAmount> {
        TODO()
    }
}
