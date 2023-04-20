package im.molly.monero

import im.molly.monero.util.decodeBase58

interface PublicAddress {
    val network: MoneroNetwork
    val subAddress: Boolean
    // viewPublicKey: ByteArray
    // spendPublicKey: ByteArray

    companion object {
        fun parse(publicAddress: String): PublicAddress {
            val decoded = try {
                publicAddress.decodeBase58()
            } catch (t: IllegalArgumentException) {
                throw InvalidAddress("Base58 decoding error", t)
            }
            if (decoded.size <= 4) {
                throw InvalidAddress("Address too short")
            }

            val prefix = decoded[0].toLong()

            StandardAddress.prefixes[prefix]?.let { network ->
                return StandardAddress(network)
            }
            SubAddress.prefixes[prefix]?.let { network ->
                TODO()
            }
            IntegratedAddress.prefixes[prefix]?.let { network ->
                TODO()
            }

            throw InvalidAddress("Unrecognized address prefix")
        }
    }
}

class InvalidAddress(message: String, cause: Throwable? = null) : Exception(message, cause)

data class StandardAddress(
    override val network: MoneroNetwork,
) : PublicAddress {
    override val subAddress = false

    companion object {
        val prefixes = mapOf(
            18L to MoneroNetwork.Mainnet,
            53L to MoneroNetwork.Testnet,
            24L to MoneroNetwork.Stagenet,
        )
    }
}

data class SubAddress(
    override val network: MoneroNetwork,
) : PublicAddress {
    override val subAddress = true

    companion object {
        val prefixes = mapOf(
            42L to MoneroNetwork.Mainnet,
            64L to MoneroNetwork.Testnet,
            36L to MoneroNetwork.Stagenet,
        )
    }
}

data class IntegratedAddress(
    override val network: MoneroNetwork,
    val paymentId: Long,
) : PublicAddress {
    override val subAddress = false

    companion object {
        val prefixes = mapOf(
            19L to MoneroNetwork.Mainnet,
            54L to MoneroNetwork.Testnet,
            25L to MoneroNetwork.Stagenet,
        )
    }
}
