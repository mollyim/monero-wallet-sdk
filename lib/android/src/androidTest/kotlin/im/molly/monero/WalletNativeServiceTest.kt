package im.molly.monero

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class WalletNativeServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private val context by lazy { InstrumentationRegistry.getInstrumentation().context }
    // private val resources by lazy { context.resources }

    @Test
    fun testBind() {
        assertThat(bindService()).isNotNull()
    }

    private fun bindService(): IWalletService {
        val binder = serviceRule.bindService(Intent(context, WalletService::class.java))
        return IWalletService.Stub.asInterface(binder)
    }
}
