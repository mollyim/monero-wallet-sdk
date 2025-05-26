package im.molly.monero.sdk.e2etest

import androidx.test.filters.LargeTest
import im.molly.monero.sdk.LedgerChainSubject
import im.molly.monero.sdk.Mainnet
import im.molly.monero.sdk.MoneroWalletSubject
import im.molly.monero.sdk.RemoteNode
import im.molly.monero.sdk.RestorePoint
import im.molly.monero.sdk.SecretKey
import im.molly.monero.sdk.service.BaseWalletService
import im.molly.monero.sdk.service.InProcessWalletService
import im.molly.monero.sdk.service.SandboxedWalletService
import im.molly.monero.sdk.singleNodeClient
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalStdlibApi::class)
abstract class WalletRefreshTest(
    serviceClass: Class<out BaseWalletService>,
) : WalletTestBase(serviceClass) {

    @Test
    fun restoredWalletEmitsExpectedLedgerOnRefresh(): Unit = runBlocking {
        val key =
            SecretKey("148d78d2aba7dbca5cd8f6abcfb0b3c009ffbdbea1ff373d50ed94d78286640e".hexToByteArray())
        val node = RemoteNode("http://node.monerodevs.org:18089", Mainnet)
        val restorePoint = RestorePoint.blockHeight(2861767)

        val wallet = walletProvider.restoreWallet(
            network = Mainnet,
            client = node.singleNodeClient(),
            secretSpendKey = key,
            restorePoint = restorePoint,
        )

        val refreshJob = launch {
            wallet.awaitRefresh()
        }

        val ledgers = withTimeout(5.minutes) {
            wallet.ledger()
                .takeWhile { it.checkedAt.height < 2862121 }
                .toList()
        }

        refreshJob.cancelAndJoin()

        LedgerChainSubject.assertThat(ledgers).hasValidWalletHistory()

        withReopenedWallet(wallet) { original, reopened ->
            MoneroWalletSubject.assertThat(reopened).matchesStateOf(original)
        }
    }
}

@LargeTest
class WalletRefreshInProcessTest : WalletRefreshTest(InProcessWalletService::class.java)

@LargeTest
class WalletRefreshSandboxedTest : WalletRefreshTest(SandboxedWalletService::class.java)
