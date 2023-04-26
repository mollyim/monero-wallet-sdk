package im.molly.monero.demo.service

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.WalletRepository
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

const val TAG = "SyncService"

class SyncService(
    private val walletRepository: WalletRepository = AppModule.walletRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LifecycleService() {

    private suspend fun doSync() = coroutineScope {
        val syncedWalletIds = mutableSetOf<Long>()

        walletRepository.getWalletIdList().collect {
            val idSet = it.toSet()
            val toSync = idSet subtract syncedWalletIds
            toSync.forEach { walletId ->
                val wallet = walletRepository.getWallet(walletId)
                launch {
                    while (isActive) {
                        val result = wallet.awaitRefresh()
                        if (result.isError()) {
                            break
                        }
                        walletRepository.saveWallet(wallet)
                        delay(10.seconds)
                    }
                }
            }
            syncedWalletIds.addAll(toSync)
        }
    }

    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: SyncService
            get() = this@SyncService
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind: $intent")
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()

        lifecycleScope.launch(ioDispatcher) {
            doSync()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            context.startService(intent)
        }
    }
}
