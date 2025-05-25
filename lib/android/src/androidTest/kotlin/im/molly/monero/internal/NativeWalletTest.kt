package im.molly.monero.internal

import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import im.molly.monero.Mainnet
import im.molly.monero.SecretKey
import im.molly.monero.Stagenet
import im.molly.monero.Testnet
import im.molly.monero.randomSecretKey
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class NativeWalletTest {

    @LargeTest
    @Test
    fun keyGenerationIsDeterministic() = runTest {
        assertThat(
            NativeWallet.localSyncWallet(
                networkId = Mainnet.id,
                secretSpendKey = SecretKey("d2ca26e22489bd9871c910c58dee3ab08e66b9d566825a064c8c0af061cd8706".hexToByteArray()),
            ).publicAddress
        ).isEqualTo("4AYjQM9HoAFNUeC3cvSfgeAN89oMMpMqiByvunzSzhn97cj726rJj3x8hCbH58UnMqQJShczCxbpWRiCJQ3HCUDHLiKuo4T")

        assertThat(
            NativeWallet.localSyncWallet(
                networkId = Testnet.id,
                secretSpendKey = SecretKey("48a35268bc33227eea43ac1ecfd144d51efc023c115c26ca68a01cc6201e9900".hexToByteArray()),
            ).publicAddress
        ).isEqualTo("A1v6gVUcGgGE87c1uFRWB1KfPVik2qLLDJiZT3rhZ8qjF3BGA6oHzeDboD23dH8rFaFFcysyqwF6DBj8WUTBWwEhESB7nZz")

        assertThat(
            NativeWallet.localSyncWallet(
                networkId = Stagenet.id,
                secretSpendKey = SecretKey("561a8d4e121ffca7321a7dc6af79679ceb4cdc8c0dcb0ef588b574586c5fac04".hexToByteArray()),
            ).publicAddress
        ).isEqualTo("54kPaUhYgGNBT72N8Bv2DFMqstLGJCEcWg1EAjwpxABkKL3uBtBLAh4VAPKvhWBdaD4ZpiftA8YWFLAxnWL4aQ9TD4vhY4W")
    }

    @LargeTest
    @Test
    fun publicAddressesAreDistinct() = runTest {
        val publicAddress =
            NativeWallet.localSyncWallet(
                networkId = Mainnet.id,
                secretSpendKey = randomSecretKey(),
            ).publicAddress

        val anotherPublicAddress =
            NativeWallet.localSyncWallet(
                networkId = Mainnet.id,
                secretSpendKey = randomSecretKey(),
            ).publicAddress

        assertThat(publicAddress).isNotEqualTo(anotherPublicAddress)
    }

    @Test
    fun balanceIsZeroAtGenesis() = runTest {
        with(
            NativeWallet.localSyncWallet(
                networkId = Mainnet.id,
                secretSpendKey = randomSecretKey(),
            ).getLedger()
        ) {
            assertThat(transactions).isEmpty()
            assertThat(isBalanceZero).isTrue()
        }
    }
}
