package im.molly.monero.sdk.e2etest

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import im.molly.monero.sdk.InMemoryWalletDataStore
import im.molly.monero.sdk.Mainnet
import im.molly.monero.sdk.MoneroWallet
import im.molly.monero.sdk.WalletDataStore
import im.molly.monero.sdk.WalletProvider
import im.molly.monero.sdk.internal.IWalletService
import im.molly.monero.sdk.internal.WalletServiceClient
import im.molly.monero.sdk.service.BaseWalletService
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class WalletTestBase(private val serviceClass: Class<out BaseWalletService>) {

    @get:Rule
    val walletServiceRule = ServiceTestRule()

    protected lateinit var walletProvider: WalletProvider
        private set

    protected val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    private fun bindService(): IWalletService {
        val binder = walletServiceRule.bindService(Intent(context, serviceClass))
        return IWalletService.Stub.asInterface(binder)
    }

    private fun unbindService() {
        walletServiceRule.unbindService()
    }

    @Before
    fun setUpBase() {
        val walletService = bindService()
        walletProvider = WalletServiceClient.withBoundService(context, walletService)
    }

    @After
    fun tearDownBase() {
        runCatching {
            walletProvider.disconnect()
        }
        unbindService()
    }

    protected suspend fun wallet(defaultStore: WalletDataStore? = null) =
        walletProvider.createNewWallet(Mainnet, defaultStore)

    protected suspend fun withReopenedWallet(
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
