package im.molly.monero

/**
 * Monero network environments.
 *
 * Defined in cryptonote_config.h
 */
enum class MoneroNetwork(val id: Int) {
    Mainnet(0),
    Testnet(1),
    Stagenet(2);

    companion object {
        fun fromId(value: Int) = values().first { it.id == value }

        fun of(publicAddress: String) = PublicAddress.parse(publicAddress).network
    }
}
