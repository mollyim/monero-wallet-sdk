package im.molly.monero

import android.os.Parcelable
import im.molly.monero.internal.constants.CRYPTONOTE_DISPLAY_DECIMAL_POINT
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@JvmInline
@Parcelize
value class MoneroAmount(val atomicUnits: Long) : Parcelable {

    companion object {
        const val ATOMIC_UNIT_SCALE: Int = CRYPTONOTE_DISPLAY_DECIMAL_POINT

        val ZERO = MoneroAmount(0)
    }

    fun toXmr(): BigDecimal = BigDecimal.valueOf(atomicUnits, ATOMIC_UNIT_SCALE)

    override fun toString() = atomicUnits.toString()

    operator fun plus(other: MoneroAmount) =
        MoneroAmount(Math.addExact(this.atomicUnits, other.atomicUnits))

    operator fun minus(other: MoneroAmount) =
        MoneroAmount(Math.subtractExact(this.atomicUnits, other.atomicUnits))

    operator fun compareTo(other: Int): Int = atomicUnits.compareTo(other)
}

fun Long.toAtomicUnits(): MoneroAmount = MoneroAmount(this)

fun Int.toAtomicUnits(): MoneroAmount = MoneroAmount(this.toLong())

inline val BigDecimal.xmr: MoneroAmount
    get() {
        val atomicUnits = times(BigDecimal.TEN.pow(MoneroAmount.ATOMIC_UNIT_SCALE)).toLong()
        return MoneroAmount(atomicUnits)
    }

inline val Double.xmr: MoneroAmount get() = BigDecimal(this).xmr

inline val Long.xmr: MoneroAmount get() = BigDecimal(this).xmr

inline val Int.xmr: MoneroAmount get() = BigDecimal(this).xmr

inline fun <T> Iterable<T>.sumOf(selector: (T) -> MoneroAmount): MoneroAmount {
    var sum: MoneroAmount = MoneroAmount.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Iterable<MoneroAmount>.sum(): MoneroAmount {
    var sum: MoneroAmount = MoneroAmount.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}
