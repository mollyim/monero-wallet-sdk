package im.molly.monero

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class Amount private constructor(
    val atomic: Long,
) : Parcelable {

    init {
        require(atomic >= 0) { "XMR amount cannot be negative" }
    }

    companion object {
        fun atomic(atomic: Long) = Amount(atomic)

        fun pico(pico: Long) = atomic(pico)
    }

    operator fun plus(other: Amount) = Amount(Math.addExact(this.atomic, other.atomic))
}

fun Long.toAtomicAmount(): Amount = Amount.atomic(this)

fun Int.toAtomicAmount(): Amount = Amount.atomic(this.toLong())

inline fun <T> Iterable<T>.sumOf(selector: (T) -> Amount): Amount {
    var sum: Amount = 0L.toAtomicAmount()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
