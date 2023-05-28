package im.molly.monero

import android.os.IInterface

/**
 * Returns whether this interface is in a remote process.
 */
fun IInterface.isRemote(): Boolean {
    return asBinder() !== this
}
