package im.molly.monero

import android.os.Parcel
import android.os.Parcelable

data class AccountAddress(
    val publicAddress: PublicAddress,
    val accountIndex: Int = 0,
    val subAddressIndex: Int = 0,
) : PublicAddress by publicAddress {

    val isPrimaryAddress: Boolean
        get() = accountIndex == 0 && subAddressIndex == 0

    init {
        when (publicAddress) {
            is StandardAddress -> require(isPrimaryAddress) {
                "Only the primary address is a standard address"
            }

            is SubAddress -> require(accountIndex != -1 && subAddressIndex != -1) {
                "Invalid subaddress indices"
            }

            else -> throw IllegalArgumentException("Unsupported address type")
        }
    }
}
