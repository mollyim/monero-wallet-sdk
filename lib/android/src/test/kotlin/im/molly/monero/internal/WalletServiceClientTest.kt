package im.molly.monero.internal

import android.content.Context
import android.content.ServiceConnection
import im.molly.monero.MoneroNetwork
import im.molly.monero.MoneroNodeClient
import im.molly.monero.RestorePoint
import im.molly.monero.WalletDataStore
import im.molly.monero.genesisTime
import im.molly.monero.randomSecretKey
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

    private val mainnet = MoneroNetwork.Mainnet
    private val testnet = MoneroNetwork.Testnet

    @Before
    fun setUp() {
        client = WalletServiceClient(context, service, serviceConnection)
    }

    @Test
    fun `throws on mismatched client network`(): Unit = runBlocking {
        val mismatchedNodeClient = mockk<MoneroNodeClient> {
            every { network } returns testnet
        }
        val dataStore = mockk<WalletDataStore>()

        assertFailsWith<IllegalArgumentException> {
            client.createNewWallet(
                network = mainnet,
                dataStore = null,
                client = mismatchedNodeClient,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            client.openWallet(
                network = mainnet,
                dataStore = dataStore,
                client = mismatchedNodeClient,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            client.restoreWallet(
                network = mainnet,
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
            every { network } returns mainnet
        }

        assertFailsWith<IllegalArgumentException> {
            client.restoreWallet(
                network = mainnet,
                dataStore = null,
                client = nodeClient,
                secretSpendKey = randomSecretKey(),
                restorePoint = testnet.genesisTime,
            )
        }
    }

    @Test
    fun `unbinds service on disconnect`() {
        client.disconnect()
        verify(exactly = 1) { context.unbindService(serviceConnection) }
    }
}
