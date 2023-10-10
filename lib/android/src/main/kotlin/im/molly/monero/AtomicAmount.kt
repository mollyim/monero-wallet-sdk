package im.molly.monero

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// TODO: Rename to MoneroAmount?

@JvmInline
@Parcelize
value class AtomicAmount(val value: Long) : Parcelable {
    operator fun plus(other: AtomicAmount) = AtomicAmount(Math.addExact(this.value, other.value))

    operator fun minus(other: AtomicAmount) = AtomicAmount(Math.subtractExact(this.value, other.value))

    operator fun compareTo(other: Int): Int = value.compareTo(other)

    companion object {
        val ZERO = AtomicAmount(0)
    }
}

fun Long.toAtomicAmount(): AtomicAmount = AtomicAmount(this)

fun Int.toAtomicAmount(): AtomicAmount = AtomicAmount(this.toLong())

inline fun <T> Iterable<T>.sumOf(selector: (T) -> AtomicAmount): AtomicAmount {
    var sum: AtomicAmount = AtomicAmount.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Iterable<AtomicAmount>.sum(): AtomicAmount {
    var sum: AtomicAmount = AtomicAmount.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}
