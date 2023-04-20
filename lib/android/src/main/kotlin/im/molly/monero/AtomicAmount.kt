package im.molly.monero

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class AtomicAmount(val value: Long) : Parcelable {
    operator fun plus(other: AtomicAmount) = AtomicAmount(Math.addExact(this.value, other.value))

    operator fun compareTo(other: Int): Int = value.compareTo(other)
}

fun Long.toAtomicAmount(): AtomicAmount = AtomicAmount(this)

fun Int.toAtomicAmount(): AtomicAmount = AtomicAmount(this.toLong())

inline fun <T> Iterable<T>.sumOf(selector: (T) -> AtomicAmount): AtomicAmount {
    var sum: AtomicAmount = 0L.toAtomicAmount()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
