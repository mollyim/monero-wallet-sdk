package im.molly.monero.internal

import android.os.ParcelFileDescriptor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HttpResponse(
    val code: Int,
    val contentType: String? = null,
    val body: ParcelFileDescriptor? = null,
) : AutoCloseable, Parcelable {
    override fun close() {
        body?.close()
    }
}
