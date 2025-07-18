package im.molly.monero.sdk.e2etest

import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import im.molly.monero.sdk.InMemoryWalletDataStore
import im.molly.monero.sdk.Mainnet
import im.molly.monero.sdk.MoneroWalletSubject
import im.molly.monero.sdk.RestorePoint
import im.molly.monero.sdk.SecretKey
import im.molly.monero.sdk.exceptions.NoSuchAccountException
import im.molly.monero.sdk.service.BaseWalletService
import im.molly.monero.sdk.service.InProcessWalletService
import im.molly.monero.sdk.service.SandboxedWalletService
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

abstract class WalletPersistenceTest(
    serviceClass: Class<out BaseWalletService>,
) : WalletTestBase(serviceClass) {

    @Test
    fun restoredWalletHasExpectedAccountKeys() = runTest {
        val sk = "148d78d2aba7dbca5cd8f6abcfb0b3c009ffbdbea1ff373d50ed94d78286640e".toSecretKey()
        val vk = "49774391fa5e8d249fc2c5b45dadef13534bf2483dede880dac88f061e809100".toSecretKey()

        val wallet = walletProvider.restoreWallet(
            network = Mainnet,
            secretSpendKey = sk,
            restorePoint = RestorePoint.creationTime(Instant.now()),
        )

        wallet.withViewKey { assertThat(it).isEqualTo(vk) }
        wallet.withSpendKey { assertThat(it).isEqualTo(sk) }
        wallet.withViewAndSpendKeys { viewKey, spendKey ->
            assertThat(viewKey).isEqualTo(vk)
            assertThat(spendKey).isEqualTo(sk)
        }

        with(wallet.publicAddress) {
            assertThat(address).isEqualTo("42ey1afDFnn4886T7196doS9GPMzexD9gXpsZJDwVjeRVdFCSoHnv7KPbBeGpzJBzHRCAs9UxqeoyFQMYbqSWYTfJJQAWDm")
            assertThat(spendPublicKey.toString()).isEqualTo("1b3bd040020d3712ab84992b773d0a965134eb2df0392fb84af95de8a17be2ab")
            assertThat(viewPublicKey.toString()).isEqualTo("231c9bf8341c6a870d92e3fb98063a90a355fb8dbf74a8561b9d7f9273247e99")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun String.toSecretKey() = SecretKey(this.hexToByteArray())

    @Test
    fun saveToMultipleStores() = runTest {
        val defaultStore = InMemoryWalletDataStore()
        val wallet = wallet(defaultStore)
        wallet.save()

        val newStore = InMemoryWalletDataStore()
        wallet.save(newStore)

        assertThat(defaultStore.toByteArray()).isEqualTo(newStore.toByteArray())
    }

    @Test
    fun getAllAccountsReturnsAll() = runTest {
        val wallet = wallet().apply {
            repeat(2) { createAccount() }
            createSubAddressForAccount(1)
        }

        val allAccounts = wallet.getAllAccounts()
        assertThat(allAccounts).hasSize(3)
        assertThat(allAccounts).containsExactlyElementsIn(
            (0..2).map { wallet.getAccount(it) }
        )

        val addresses = allAccounts.map { it.addresses }
        assertThat(addresses[0]).hasSize(1)
        assertThat(addresses[1]).hasSize(2)
        assertThat(addresses[2]).hasSize(1)

        allAccounts.forEach { acc ->
            val primary = acc.addresses[0]
            assertThat(primary.isPrimaryAddress).isTrue()
            assertThat(primary.subAddressIndex).isEqualTo(0)
            if (acc.accountIndex == 1) {
                val sub = acc.addresses[1]
                assertThat(sub.isPrimaryAddress).isFalse()
                assertThat(sub.subAddressIndex).isEqualTo(1)
            }
        }

        withReopenedWallet(wallet) { original, reopened ->
            MoneroWalletSubject.assertThat(reopened).matchesStateOf(original)
        }
    }

    @Test(expected = NoSuchAccountException::class)
    fun getAccountThrowsForMissingAccount() = runTest {
        wallet().getAccount(accountIndex = 42)
    }

    @Test(expected = NoSuchAccountException::class)
    fun createSubAddressForAccountThrowsForMissingAccount() = runTest {
        wallet().createSubAddressForAccount(accountIndex = 42)
    }

    @Test(expected = NoSuchAccountException::class)
    fun findUnusedSubAddressThrowsForMissingAccount() = runTest {
        wallet().findUnusedSubAddress(accountIndex = 42)
    }
}

@LargeTest
class WalletPersistenceInProcessTest : WalletPersistenceTest(InProcessWalletService::class.java)

@LargeTest
class WalletPersistenceSandboxedTest : WalletPersistenceTest(SandboxedWalletService::class.java)
