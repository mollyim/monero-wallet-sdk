package im.molly.monero

import android.os.ParcelFileDescriptor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HttpResponse
@CalledByNative("http_client.cc")
constructor(
    val code: Int,
    val contentType: String? = null,
    val body: ParcelFileDescriptor? = null,
) : Parcelable
