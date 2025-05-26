package im.molly.monero.sdk.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.molly.monero.sdk.internal.IWalletService
import im.molly.monero.sdk.internal.NativeWalletService

abstract class BaseWalletService : LifecycleService() {
    private val service: IWalletService by lazy {
        NativeWalletService(this, lifecycleScope)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return service.asBinder()
    }
}
