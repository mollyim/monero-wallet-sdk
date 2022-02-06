package im.molly.monero

import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WalletNativeTest {

    @LargeTest
    @Test
    fun keyGenerationIsDeterministic() {
        assertThat(
            WalletNative.fullNode(MoneroNetwork.Mainnet.id) {
                secretSpendKey(SecretKey("d2ca26e22489bd9871c910c58dee3ab08e66b9d566825a064c8c0af061cd8706".parseHex()))
            }.publicAddress
        ).isEqualTo("4AYjQM9HoAFNUeC3cvSfgeAN89oMMpMqiByvunzSzhn97cj726rJj3x8hCbH58UnMqQJShczCxbpWRiCJQ3HCUDHLiKuo4T")

        assertThat(
            WalletNative.fullNode(MoneroNetwork.Testnet.id) {
                secretSpendKey(SecretKey("48a35268bc33227eea43ac1ecfd144d51efc023c115c26ca68a01cc6201e9900".parseHex()))
            }.publicAddress
        ).isEqualTo("A1v6gVUcGgGE87c1uFRWB1KfPVik2qLLDJiZT3rhZ8qjF3BGA6oHzeDboD23dH8rFaFFcysyqwF6DBj8WUTBWwEhESB7nZz")

        assertThat(
            WalletNative.fullNode(MoneroNetwork.Stagenet.id) {
                secretSpendKey(SecretKey("561a8d4e121ffca7321a7dc6af79679ceb4cdc8c0dcb0ef588b574586c5fac04".parseHex()))
            }.publicAddress
        ).isEqualTo("54kPaUhYgGNBT72N8Bv2DFMqstLGJCEcWg1EAjwpxABkKL3uBtBLAh4VAPKvhWBdaD4ZpiftA8YWFLAxnWL4aQ9TD4vhY4W")
    }

    @LargeTest
    @Test
    fun publicAddressesAreDistinct() {
        val publicAddress =
            WalletNative.fullNode(MoneroNetwork.Mainnet.id) {
                secretSpendKey(randomSecretKey())
            }.publicAddress

        val anotherPublicAddress =
            WalletNative.fullNode(MoneroNetwork.Mainnet.id) {
                secretSpendKey(randomSecretKey())
            }.publicAddress

        assertThat(publicAddress).isNotEqualTo(anotherPublicAddress)
    }

    @Test
    fun atGenesisBalanceIsZero() {
        with(
            WalletNative.fullNode(MoneroNetwork.Mainnet.id) {
                secretSpendKey(randomSecretKey())
            }.currentBalance
        ) {
            assertThat(totalAmount).isEqualTo(0.toAtomicAmount())
            assertThat(totalAmountUnlockedAt(1)).isEqualTo(0.toAtomicAmount())
        }
    }
}
