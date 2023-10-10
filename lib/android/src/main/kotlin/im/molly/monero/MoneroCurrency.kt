package im.molly.monero

import im.molly.monero.internal.constants.CRYPTONOTE_DISPLAY_DECIMAL_POINT
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

object MoneroCurrency {
    const val symbol = "XMR"

    fun format(atomicAmount: AtomicAmount, formatter: NumberFormat = DefaultFormatter): String =
        formatter.format(BigDecimal.valueOf(atomicAmount.value, CRYPTONOTE_DISPLAY_DECIMAL_POINT))

    fun parse(source: String, formatter: NumberFormat = DefaultFormatter): AtomicAmount {
        TODO()
    }
}

val DefaultFormatter: NumberFormat = NumberFormat.getInstance(Locale.US).apply {
    minimumFractionDigits = 5
}
