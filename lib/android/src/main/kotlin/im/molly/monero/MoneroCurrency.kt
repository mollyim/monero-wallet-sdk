package im.molly.monero

import java.text.NumberFormat
import java.util.Locale

object MoneroCurrency {
    const val SYMBOL = "XMR"

    const val MAX_PRECISION = MoneroAmount.ATOMIC_UNIT_SCALE

    val DefaultFormat = Format()

    data class Format(
        val precision: Int = MAX_PRECISION,
        val locale: Locale = Locale.US,
    ) {
        init {
            require(precision in 0..MAX_PRECISION) {
                "Precision must be between 0 and $MAX_PRECISION"
            }
        }

        private val numberFormat = NumberFormat.getInstance(locale).apply {
            minimumFractionDigits = precision
        }

        fun format(amount: MoneroAmount): String = numberFormat.format(amount.toXmr())

        fun parse(source: String): MoneroAmount {
            TODO()
        }
    }
}
