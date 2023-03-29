package im.molly.monero.demo.service

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.WalletRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class SyncService(
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : LifecycleService() {
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: SyncService
            get() = this@SyncService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            val syncedWalletIds = mutableSetOf<Long>()
            walletRepository.getWalletIdList().collect {
                val idSet = it.toSet()
                val toSync = idSet subtract syncedWalletIds
                toSync.forEach { walletId ->
                    val wallet = walletRepository.getWallet(walletId)
                    lifecycleScope.launch {
                        while (isActive) {
                            val result = wallet.awaitRefresh()
                            if (result.isError()) {
                                break
                            }
                            delay(10.seconds)
                        }
                    }
                }
                syncedWalletIds.addAll(toSync)
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            context.startService(intent)
        }
    }
}
