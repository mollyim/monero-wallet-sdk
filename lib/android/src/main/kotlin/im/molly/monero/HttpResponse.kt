package im.molly.monero

import android.os.ParcelFileDescriptor

data class HttpResponse(
    val code: Int,
    val contentType: String? = null,
    val body: ParcelFileDescriptor? = null,
) : AutoCloseable {
    override fun close() {
        body?.close()
    }
}
