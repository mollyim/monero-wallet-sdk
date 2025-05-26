package im.molly.monero.sdk

import android.os.Parcelable
import im.molly.monero.sdk.util.decodeBase58
import kotlinx.parcelize.Parcelize

sealed interface PublicAddress : Parcelable {
    val address: String
    val network: MoneroNetwork
    // viewPublicKey: ByteArray
    // spendPublicKey: ByteArray

    fun isSubAddress(): Boolean

    companion object {
        /**
         * @throws InvalidAddress
         */
        fun parse(addressString: String): PublicAddress {
            val decoded = try {
                addressString.decodeBase58()
            } catch (t: IllegalArgumentException) {
                throw InvalidAddress("Base58 decoding error", t)
            }
            if (decoded.size <= 4) {
                throw InvalidAddress("Address too short")
            }

            return when (val prefix = decoded[0].toLong()) {
                in StandardAddress.prefixes -> {
                    StandardAddress(addressString, StandardAddress.prefixes[prefix]!!)
                }

                in SubAddress.prefixes -> {
                    SubAddress(addressString, SubAddress.prefixes[prefix]!!)
                }

                in IntegratedAddress.prefixes -> {
                    TODO()
                }

                else -> throw InvalidAddress("Unrecognized address prefix")
            }
        }
    }
}

// TODO: Extend ParseException
class InvalidAddress(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

@Parcelize
data class StandardAddress(
    override val address: String,
    override val network: MoneroNetwork,
) : PublicAddress {

    companion object {
        val prefixes = mapOf(
            18L to Mainnet,
            53L to Testnet,
            24L to Stagenet,
        )
    }

    override fun isSubAddress() = false

    override fun toString(): String = address
}

@Parcelize
data class SubAddress(
    override val address: String,
    override val network: MoneroNetwork,
) : PublicAddress {

    companion object {
        val prefixes = mapOf(
            42L to Mainnet,
            63L to Testnet,
            36L to Stagenet,
        )
    }

    override fun isSubAddress() = true

    override fun toString(): String = address
}

@Parcelize
data class IntegratedAddress(
    override val address: String,
    override val network: MoneroNetwork,
    val paymentId: Long,
) : PublicAddress {

    companion object {
        val prefixes = mapOf(
            19L to Mainnet,
            54L to Testnet,
            25L to Stagenet,
        )
    }

    override fun isSubAddress() = false

    override fun toString(): String = address
}
