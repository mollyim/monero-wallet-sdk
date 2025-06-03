package im.molly.monero.sdk

import com.google.common.truth.Truth.assertThat
import im.molly.monero.sdk.util.decodeBase58
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.random.Random

@RunWith(Parameterized::class)
class PublicAddressParsingTest(
    private val expectedNetwork: MoneroNetwork,
    private val isSubAddress: Boolean,
    private val address: String,
    private val spendPublicKey: String,
    private val viewPublicKey: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Array<Any>> = listOf(
            arrayOf(
                Mainnet, false,
                "44Kbx4sJ7JDRDV5aAhLJzQCjDz2ViLRduE3ijDZu3osWKBjMGkV1XPk4pfDUMqt1Aiezvephdqm6YD19GKFD9ZcXVUTp6BW",
                "47335f3ceae62690c602dc20cdb6bd461dfb409f7322844e0092dbb4000c796c",
                "b8954f72ccc4bf16d93600d0cfba6be32def0ca114bf7147c20c42769bef4cfc",
            ),
            arrayOf(
                Testnet, false,
                "9ujeXrjzf7bfeK3KZdCqnYaMwZVFuXemPU8Ubw335rj2FN1CdMiWNyFV3ksEfMFvRp9L9qum5UxkP5rN9aLcPxbH1au4WAB",
                "43ca04c0bac1fee7087d0779959c89c773e1d4d4a477f2a2316cb431018ee955",
                "dd951a02750dcaa7af680fd3fd148331cd980eda5e1d881d00bf1e35865f4005",
            ),
            arrayOf(
                Stagenet, false,
                "53teqCAESLxeJ1REzGMAat1ZeHvuajvDiXqboEocPaDRRmqWoVPzy46GLo866qRFjbNhfkNckyhST3WEvBviDwpUDd7DSzB",
                "365f7c1aa6cc01def62e128fffd8e1035d64cea20211b5b85e313737f28e1494",
                "1bbc4ca71a085b5bb8390ab800b53e81be2abab23f63740ef8a450804c6de96f",
            ),
            arrayOf(
                Mainnet, true,
                "86kKnBKFqzCLxtK1Jmx2BkNBDBSMDEVaRYMMyVbeURYDWs8uNGDZURKCA5yRcyMxHzPcmCf1q2fSdhQVcaKsFrtGRsdGfNk",
                "71521fb4561485775aa8e3b2398e8b7e9a6dcfb70da20abb6f8dae06d43a3ab2",
                "8c3572ef0c82ae42b38586c7404e1587373aef49d6d94ef190e8feec603fd1dc"
            ),
            arrayOf(
                Testnet, true,
                "BcFvPa3fT4gVt5QyRDe5Vv7VtUFao9ci8NFEy3r254KF7R1N2cNB5FYhGvrHbMStv4D6VDzZ5xtxeKV8vgEPMnDcNFuwZb9",
                "6b9ed65b32362dacaa7c48c8a7f82526d96f599897fefd7f04cb8b7fd4ea5e26",
                "58a1145569ef5bf0c949048a6ede19484f1221a2ba01df6e83e95741d2dd0fbc",
            ),
            arrayOf(
                Stagenet, true,
                "7A1Hr63MfgUa8pkWxueD5xBqhQczkusYiCMYMnJGcGmuQxa7aDBxN1G7iCuLCNB3VPeb2TW7U9FdxB27xKkWKfJ8VhUZthF",
                "ccc5703d9109e9c619bc427e9874f740ce43c25e5466e743e1cc4a6cf6d4908f",
                "3c79ff40b5b8fb281e7b379a652c36e0b74129684f43473be6cac960f124b9fe",
            ),
        )
    }

    @Test
    fun `parse returns correct address type, network, and keys`() {
        val parsed = PublicAddress.parse(address)

        assertThat(parsed.address).isEqualTo(address)
        assertThat(parsed.toString()).isEqualTo(address)
        assertThat(parsed.network).isEqualTo(expectedNetwork)
        assertThat(parsed.isSubAddress()).isEqualTo(isSubAddress)
        assertThat(parsed.spendPublicKey.toString()).isEqualTo(spendPublicKey)
        assertThat(parsed.viewPublicKey.toString()).isEqualTo(viewPublicKey)

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
        mockkStatic("im.molly.monero.sdk.util.Base58Kt")

        val unknownPrefix = "unknownprefix"
        val padding = Random.nextBytes(68)
        every { unknownPrefix.decodeBase58() } returns byteArrayOf(99) + padding

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
