package im.molly.monero.internal

import android.os.Build
import android.os.IBinder
import android.os.IInterface

/**
 * Returns whether this interface is in a remote process.
 */
fun IInterface.isRemote(): Boolean {
    return asBinder() !== this
}

fun getMaxIpcSize(): Int = if (Build.VERSION.SDK_INT >= 30) {
    IBinder.getSuggestedMaxIpcSizeBytes()
} else {
    64 * 1024
}
