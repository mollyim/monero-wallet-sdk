package im.molly.monero.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.molly.monero.internal.IWalletService
import im.molly.monero.internal.NativeWalletService

abstract class BaseWalletService(private val isolated: Boolean) : LifecycleService() {
    private val service: IWalletService by lazy {
        NativeWalletService(lifecycleScope).apply {
            if (isolated) {
                configureLoggingAdapter()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return service.asBinder()
    }
}
