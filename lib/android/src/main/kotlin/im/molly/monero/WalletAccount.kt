package im.molly.monero

data class WalletAccount(
    val addresses: List<AccountAddress>,
    val accountIndex: Int,
)

fun Iterable<WalletAccount>.findAddressByIndex(
    accountIndex: Int,
    subAddressIndex: Int = 0,
): AccountAddress? {
    return flatMap { it.addresses }.find {
        it.accountIndex == accountIndex && it.subAddressIndex == subAddressIndex
    }
}
