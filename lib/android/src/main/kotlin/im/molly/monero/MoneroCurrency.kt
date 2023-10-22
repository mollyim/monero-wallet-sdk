package im.molly.monero

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

object MoneroCurrency {
    const val SYMBOL = "XMR"

    const val MAX_PRECISION = MoneroAmount.ATOMIC_UNIT_SCALE

    open class Format(
        val precision: Int,
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

        open fun format(amount: MoneroAmount): String {
            return numberFormat.format(amount.xmr)
        }

        open fun parse(source: String): MoneroAmount {
            TODO()
        }
    }

    val ExactFormat = object : Format(MoneroAmount.ATOMIC_UNIT_SCALE) {
        override fun format(amount: MoneroAmount) = buildString {
            if (amount.atomicUnits < 0) {
                append('-')
            }

            val num = amount.atomicUnits.absoluteValue.toString()

            if (precision < num.length) {
                val point = num.length - precision
                append(num.substring(0, point))
                append('.')
                append(num.substring(point))
            } else {
                append("0.")
                for (i in 1..(precision - num.length)) {
                    append('0')
                }
                append(num)
            }
        }
    }

    fun format(amount: MoneroAmount, outputFormat: Format = ExactFormat): String {
        return outputFormat.format(amount)
    }

    fun format(amount: MoneroAmount, precision: Int): String {
        return Format(precision = precision).format(amount)
    }
}
