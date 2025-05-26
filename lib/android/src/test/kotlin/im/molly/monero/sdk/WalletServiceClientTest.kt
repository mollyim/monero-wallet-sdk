package im.molly.monero.sdk

import android.content.Context
import android.content.ServiceConnection
import im.molly.monero.sdk.internal.IWalletService
import im.molly.monero.sdk.internal.WalletServiceClient
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith

class WalletServiceClientTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK(relaxed = true)
    lateinit var context: Context

    @MockK
    lateinit var service: IWalletService

    @MockK
    lateinit var serviceConnection: ServiceConnection

    private lateinit var client: WalletServiceClient

    @Before
    fun setUp() {
        client = WalletServiceClient(context, service, serviceConnection)
    }

    @Test
    fun `throws on mismatched client network`(): Unit = runBlocking {
        val mismatchedNodeClient = mockk<MoneroNodeClient> {
            every { network } returns Testnet
        }
        val dataStore = mockk<WalletDataStore>()

        assertFailsWith<IllegalArgumentException> {
            client.createNewWallet(
                network = Mainnet,
                dataStore = null,
                client = mismatchedNodeClient,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            client.openWallet(
                network = Mainnet,
                dataStore = dataStore,
                client = mismatchedNodeClient,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            client.restoreWallet(
                network = Mainnet,
                dataStore = null,
                client = mismatchedNodeClient,
                secretSpendKey = randomSecretKey(),
                restorePoint = RestorePoint.blockHeight(1),
            )
        }
    }

    @Test
    fun `throws on mismatched restore point network`(): Unit = runBlocking {
        val nodeClient = mockk<MoneroNodeClient> {
            every { network } returns Mainnet
        }

        assertFailsWith<IllegalArgumentException> {
            client.restoreWallet(
                network = Mainnet,
                dataStore = null,
                client = nodeClient,
                secretSpendKey = randomSecretKey(),
                restorePoint = Testnet.genesisTime,
            )
        }
    }

    @Test
    fun `unbinds service on disconnect`() {
        client.disconnect()
        verify(exactly = 1) { context.unbindService(serviceConnection) }
    }
}
