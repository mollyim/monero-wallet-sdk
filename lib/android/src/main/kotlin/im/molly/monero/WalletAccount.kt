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

fun parseAndAggregateAddresses(addresses: Iterable<String>): List<WalletAccount> {
    return addresses
        .map { AccountAddress.parseWithIndexes(it) }
        .groupBy { it.accountIndex }
        .toSortedMap()
        .map { (index, addresses) -> WalletAccount(addresses, index) }
}
