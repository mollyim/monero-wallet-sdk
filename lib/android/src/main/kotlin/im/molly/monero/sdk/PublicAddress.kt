package im.molly.monero.sdk

import android.os.Parcelable
import im.molly.monero.sdk.util.decodeBase58
import kotlinx.parcelize.Parcelize

sealed interface PublicAddress : Parcelable {
    val address: String
    val network: MoneroNetwork
    val spendPublicKey: PublicKey
    val viewPublicKey: PublicKey

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

            if (decoded.size < 69) {
                throw InvalidAddress("Address too short")
            }

            val spendKey = PublicKey(decoded.sliceArray(1 until 33))
            val viewKey = PublicKey(decoded.sliceArray(33 until 65))

            return when (val prefix = decoded[0].toLong()) {
                in StandardAddress.prefixes -> {
                    StandardAddress(
                        address = addressString,
                        network = StandardAddress.prefixes.getValue(prefix),
                        spendPublicKey = spendKey,
                        viewPublicKey = viewKey,
                    )
                }

                in SubAddress.prefixes -> {
                    SubAddress(
                        address = addressString,
                        network = SubAddress.prefixes.getValue(prefix),
                        spendPublicKey = spendKey,
                        viewPublicKey = viewKey,
                    )
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
    override val spendPublicKey: PublicKey,
    override val viewPublicKey: PublicKey,
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
    override val spendPublicKey: PublicKey,
    override val viewPublicKey: PublicKey,
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
    override val spendPublicKey: PublicKey,
    override val viewPublicKey: PublicKey,
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
