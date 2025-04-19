package im.molly.monero

import com.google.common.truth.Truth.assertThat
import im.molly.monero.util.decodeBase58
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PublicAddressParsingTest(
    private val expectedNetwork: MoneroNetwork,
    private val isSubAddress: Boolean,
    private val address: String,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Array<Any>> = listOf(
            arrayOf(MoneroNetwork.Mainnet, false, "42ey1afDFnn4886T7196doS9GPMzexD9gXpsZJDwVjeRVdFCSoHnv7KPbBeGpzJBzHRCAs9UxqeoyFQMYbqSWYTfJJQAWDm"),
            arrayOf(MoneroNetwork.Mainnet, false, "44Kbx4sJ7JDRDV5aAhLJzQCjDz2ViLRduE3ijDZu3osWKBjMGkV1XPk4pfDUMqt1Aiezvephdqm6YD19GKFD9ZcXVUTp6BW"),
            arrayOf(MoneroNetwork.Testnet, false, "9ujeXrjzf7bfeK3KZdCqnYaMwZVFuXemPU8Ubw335rj2FN1CdMiWNyFV3ksEfMFvRp9L9qum5UxkP5rN9aLcPxbH1au4WAB"),
            arrayOf(MoneroNetwork.Stagenet, false, "53teqCAESLxeJ1REzGMAat1ZeHvuajvDiXqboEocPaDRRmqWoVPzy46GLo866qRFjbNhfkNckyhST3WEvBviDwpUDd7DSzB"),
            arrayOf(MoneroNetwork.Mainnet, true, "8AsN91rznfkBGTY8psSNkJBg9SZgxxGGRUhGwRptBhgr5XSQ1XzmA9m8QAnoxydecSh5aLJXdrgXwTDMMZ1AuXsN1EX5Mtm"),
            arrayOf(MoneroNetwork.Mainnet, true, "86kKnBKFqzCLxtK1Jmx2BkNBDBSMDEVaRYMMyVbeURYDWs8uNGDZURKCA5yRcyMxHzPcmCf1q2fSdhQVcaKsFrtGRsdGfNk"),
            arrayOf(MoneroNetwork.Testnet, true,"BdKg9udkvckC5T58a8Nmtb6BNsgRAxs7uA2D49sWNNX5HPW5Us6Wxu8QMXrnSx3xPBQQ2iu9kwEcRGAoiz6EPmcZKbF62GS"),
            arrayOf(MoneroNetwork.Testnet, true, "BcFvPa3fT4gVt5QyRDe5Vv7VtUFao9ci8NFEy3r254KF7R1N2cNB5FYhGvrHbMStv4D6VDzZ5xtxeKV8vgEPMnDcNFuwZb9"),
            arrayOf(MoneroNetwork.Stagenet, true, "73LhUiix4DVFMcKhsPRG51QmCsv8dYYbL6GcQoLwEEFvPvkVvc7BhebfA4pnEFF9Lq66hwvLqBvpHjTcqvpJMHmmNjPPBqa"),
            arrayOf(MoneroNetwork.Stagenet, true, "7A1Hr63MfgUa8pkWxueD5xBqhQczkusYiCMYMnJGcGmuQxa7aDBxN1G7iCuLCNB3VPeb2TW7U9FdxB27xKkWKfJ8VhUZthF"),
        )
    }

    @Test
    fun `parse determines correct type and network`() {
        val parsed = PublicAddress.parse(address)

        assertThat(parsed.address).isEqualTo(address)
        assertThat(parsed.toString()).isEqualTo(address)
        assertThat(parsed.network).isEqualTo(expectedNetwork)
        assertThat(parsed.isSubAddress()).isEqualTo(isSubAddress)

        when (parsed) {
            is SubAddress -> assertThat(isSubAddress).isTrue()
            is StandardAddress -> assertThat(isSubAddress).isFalse()
            else -> error("Unexpected address type: ${parsed::class}")
        }
    }
}

class PublicAddressExceptionTest {

    @Test
    fun `throws InvalidAddress on unknown prefix`() {
        mockkStatic("im.molly.monero.util.Base58Kt")

        val unknownPrefix = "unknownprefix"
        every { unknownPrefix.decodeBase58() } returns byteArrayOf(99, 1, 2, 3, 4, 5)

        val thrown = Assert.assertThrows(InvalidAddress::class.java) {
            PublicAddress.parse(unknownPrefix)
        }
        assertThat(thrown.message).contains("Unrecognized address prefix")
    }

    @Test
    fun `throws InvalidAddress when address is too short`() {
        val thrown = Assert.assertThrows(InvalidAddress::class.java) {
            PublicAddress.parse("111")
        }
        assertThat(thrown.message).contains("Address too short")
    }

    @Test
    fun `throws InvalidAddress on decoding error`() {
        val thrown = Assert.assertThrows(InvalidAddress::class.java) {
            PublicAddress.parse("zz")
        }
        assertThat(thrown.message).contains("Base58 decoding error")
    }
}
