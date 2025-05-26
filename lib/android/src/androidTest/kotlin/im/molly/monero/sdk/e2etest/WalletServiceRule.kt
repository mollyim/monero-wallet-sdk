package im.molly.monero.sdk.e2etest

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import im.molly.monero.sdk.WalletProvider
import im.molly.monero.sdk.internal.IWalletService
import im.molly.monero.sdk.internal.WalletServiceClient
import im.molly.monero.sdk.service.BaseWalletService
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class WalletServiceRule(private val serviceClass: Class<out BaseWalletService>) : TestRule {

    val walletProvider: WalletProvider
        get() = _walletProvider ?: error("WalletService not bound yet")

    private var _walletProvider: WalletProvider? = null

    private val context: Context by lazy { InstrumentationRegistry.getInstrumentation().context }

    private val delegate = ServiceTestRule()

    override fun apply(base: Statement, description: Description): Statement {
        return delegate.apply(object : Statement() {
            override fun evaluate() {
                val binder = delegate.bindService(Intent(context, serviceClass))
                val walletService = IWalletService.Stub.asInterface(binder)
                _walletProvider = WalletServiceClient.withBoundService(context, walletService)

                try {
                    walletProvider.use { base.evaluate() }
                } finally {
                    delegate.unbindService()
                }
            }
        }, description)
    }
}
