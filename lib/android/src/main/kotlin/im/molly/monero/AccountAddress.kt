package im.molly.monero

data class AccountAddress(
    val publicAddress: PublicAddress,
    val accountIndex: Int = 0,
    val subAddressIndex: Int = 0,
) : PublicAddress by publicAddress {

    init {
        when (publicAddress) {
            is SubAddress -> require(accountIndex != -1 && subAddressIndex != -1)
            else -> require(accountIndex == 0 && subAddressIndex == 0)
        }
    }

    fun belongsTo(targetAccountIndex: Int): Boolean {
        return accountIndex == targetAccountIndex
    }
}
