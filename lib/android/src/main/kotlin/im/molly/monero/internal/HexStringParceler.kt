package im.molly.monero.internal

import android.os.Parcel
import kotlinx.parcelize.Parceler

@OptIn(ExperimentalStdlibApi::class)
object HexStringParceler : Parceler<String?> {
    override fun create(parcel: Parcel): String? =
        parcel.createByteArray()?.toHexString()

    override fun String?.write(parcel: Parcel, flags: Int) {
        parcel.writeByteArray(this?.hexToByteArray())
    }
}
