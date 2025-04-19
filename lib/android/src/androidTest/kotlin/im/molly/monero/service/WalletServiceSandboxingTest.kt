package im.molly.monero.service

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WalletServiceSandboxingTest {

    private val context: Context by lazy { InstrumentationRegistry.getInstrumentation().context }

    @Test
    fun inProcessWalletServiceIsNotIsolated() = runTest {
        InProcessWalletService.connect(context).use {  walletProvider ->
            assertThat(walletProvider.isServiceIsolated()).isFalse()
        }
    }

    @Test
    fun sandboxedWalletServiceIsIsolated() = runTest {
        SandboxedWalletService.connect(context).use { walletProvider ->
            assertThat(walletProvider.isServiceIsolated()).isTrue()
        }
    }
}
