package im.molly.monero

import im.molly.monero.util.decodeBase58

sealed interface PublicAddress {
    val address: String
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

            return when (val prefix = decoded[0].toLong()) {
                in StandardAddress.prefixes -> {
                    StandardAddress(publicAddress, StandardAddress.prefixes[prefix]!!)
                }
                in SubAddress.prefixes -> {
                    SubAddress(publicAddress, SubAddress.prefixes[prefix]!!)
                }
                in IntegratedAddress.prefixes -> {
                    TODO()
                }
                else -> throw InvalidAddress("Unrecognized address prefix")
            }
        }
    }
}

class InvalidAddress(message: String, cause: Throwable? = null) : Exception(message, cause)

data class StandardAddress(
    override val address: String,
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

    override fun toString(): String = address
}

data class SubAddress(
    override val address: String,
    override val network: MoneroNetwork,
) : PublicAddress {
    override val subAddress = true

    companion object {
        val prefixes = mapOf(
            42L to MoneroNetwork.Mainnet,
            63L to MoneroNetwork.Testnet,
            36L to MoneroNetwork.Stagenet,
        )
    }

    override fun toString(): String = address
}

data class IntegratedAddress(
    override val address: String,
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

    override fun toString(): String = address
}
