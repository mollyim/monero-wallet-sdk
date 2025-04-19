package im.molly.monero.e2etest

import android.content.Context
import android.content.Intent
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.google.common.truth.Truth.assertThat
import im.molly.monero.InMemoryWalletDataStore
import im.molly.monero.MoneroNetwork
import im.molly.monero.MoneroWallet
import im.molly.monero.MoneroWalletSubject
import im.molly.monero.RestorePoint
import im.molly.monero.SecretKey
import im.molly.monero.WalletProvider
import im.molly.monero.internal.IWalletService
import im.molly.monero.internal.WalletServiceClient
import im.molly.monero.mnemonics.MoneroMnemonic
import im.molly.monero.mnemonics.toSecretKey
import im.molly.monero.service.BaseWalletService
import im.molly.monero.service.InProcessWalletService
import im.molly.monero.service.SandboxedWalletService
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@LargeTest
abstract class MoneroWalletTest(private val serviceClass: Class<out BaseWalletService>) {

    @get:Rule
    val walletServiceRule = ServiceTestRule()

    private lateinit var walletProvider: WalletProvider

    private val context: Context by lazy { InstrumentationRegistry.getInstrumentation().context }

    private fun bindService(): IWalletService {
        val binder = walletServiceRule.bindService(Intent(context, serviceClass))
        return IWalletService.Stub.asInterface(binder)
    }

    private fun unbindService() {
        walletServiceRule.unbindService()
    }

    @Before
    fun setUp() {
        val walletService = bindService()
        walletProvider = WalletServiceClient.withBoundService(context, walletService)
    }

    @After
    fun tearDown() {
        walletProvider.disconnect()
        unbindService()
    }

    @Test
    fun restoredWalletHasExpectedAddress() = runTest {
        val key =
            MoneroMnemonic.recoverEntropy(
                "velvet lymph giddy number token physics poetry unquoted nibs useful sabotage limits benches lifestyle eden nitrogen anvil fewest avoid batch vials washing fences goat unquoted"
            )?.toSecretKey()
        val wallet = walletProvider.restoreWallet(
            network = MoneroNetwork.Mainnet,
            secretSpendKey = key ?: error("recoverEntropy failed"),
            restorePoint = RestorePoint.creationTime(Instant.now()),
        )
        assertThat(wallet.publicAddress.address)
            .isEqualTo("42ey1afDFnn4886T7196doS9GPMzexD9gXpsZJDwVjeRVdFCSoHnv7KPbBeGpzJBzHRCAs9UxqeoyFQMYbqSWYTfJJQAWDm")
    }

    @Test
    fun saveToMultipleStores() = runTest {
        val defaultStore = InMemoryWalletDataStore()
        val wallet = walletProvider.createNewWallet(MoneroNetwork.Mainnet, defaultStore)
        wallet.save()

        val newStore = InMemoryWalletDataStore()
        wallet.save(newStore)

        assertThat(defaultStore.toByteArray()).isEqualTo(newStore.toByteArray())
    }

    @Test
    fun createAccountAndSave() = runTest {
        val wallet = walletProvider.createNewWallet(MoneroNetwork.Mainnet)
        val newAccount = wallet.createAccount()

        withReopenedWallet(wallet) { original, reopened ->
            MoneroWalletSubject.assertThat(reopened).matchesStateOf(original)
        }
    }

    private suspend fun withReopenedWallet(
        wallet: MoneroWallet,
        action: suspend (original: MoneroWallet, reopened: MoneroWallet) -> Unit,
    ) {
        walletProvider.openWallet(
            network = wallet.network,
            dataStore = InMemoryWalletDataStore().apply {
                wallet.save(targetStore = this)
            },
        ).use { reopened ->
            action(wallet, reopened)
        }
    }
}

class MoneroWalletInProcessTest : MoneroWalletTest(InProcessWalletService::class.java)
class MoneroWalletSandboxedTest : MoneroWalletTest(SandboxedWalletService::class.java)
