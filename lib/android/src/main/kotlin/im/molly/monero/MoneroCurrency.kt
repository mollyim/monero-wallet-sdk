package im.molly.monero

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

object MoneroCurrency {
    const val symbol = "XMR"

    fun format(atomicAmount: AtomicAmount, formatter: NumberFormat = DefaultFormatter): String =
        formatter.format(BigDecimal.valueOf(atomicAmount.value, 12))

    fun parse(source: String, formatter: NumberFormat = DefaultFormatter): AtomicAmount {
        TODO()
    }
}

val DefaultFormatter: NumberFormat = NumberFormat.getInstance(Locale.US).apply {
    minimumFractionDigits = 5
}
