package im.molly.monero

import android.annotation.SuppressLint

@SuppressLint("ParcelCreator")
data class AccountAddress(
    val publicAddress: PublicAddress,
    val accountIndex: Int = 0,
    val subAddressIndex: Int = 0,
) : PublicAddress by publicAddress {

    val isPrimaryAddress: Boolean
        get() = subAddressIndex == 0

    init {
        when (publicAddress) {
            is StandardAddress -> require(accountIndex == 0 && subAddressIndex == 0) {
                "Only the account address 0/0 is a standard address"
            }

            is SubAddress -> require(accountIndex != -1 && subAddressIndex != -1) {
                "Invalid subaddress indices"
            }

            else -> throw IllegalArgumentException("Unsupported address type")
        }
    }

    companion object {
        fun parseWithIndexes(addressString: String): AccountAddress {
            val parts = addressString.split("/")
            require(parts.size == 3) { "Invalid account address format" }
            val accountIndex = parts[0].toInt()
            val subAddressIndex = parts[1].toInt()
            val publicAddress = PublicAddress.parse(parts[2])
            return AccountAddress(publicAddress, accountIndex, subAddressIndex)
        }
    }

    override fun toString(): String = "$accountIndex/$subAddressIndex/$publicAddress"
}

fun Iterable<AccountAddress>.findByIndexes(
    accountIndex: Int,
    subAddressIndex: Int,
): AccountAddress? {
    return find { it.accountIndex == accountIndex && it.subAddressIndex == subAddressIndex }
}
