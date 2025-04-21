package im.molly.monero.demo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.WalletRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

const val TAG = "SyncService"

const val NOTIFICATION_CHANNEL_ID = "SyncService"

class SyncService(
    private val walletRepository: WalletRepository = AppModule.walletRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LifecycleService() {

    private suspend fun doSync() = coroutineScope {
        val syncedWalletIds = mutableSetOf<Long>()
        val mutex = Mutex()

        launch {
            while (isActive) {
                mutex.withLock {
                    syncedWalletIds.map {
                        walletRepository.getWallet(it)
                    }.forEach { wallet ->
                        wallet.save()
                    }
                }
                delay(60.seconds)
            }
        }

        walletRepository.getWalletIdList().collect {
            val idSet = it.toSet()
            val toSync = idSet subtract syncedWalletIds
            toSync.forEach { walletId ->
                val wallet = walletRepository.getWallet(walletId)
                launch {
                    while (isActive) {
                        val result = wallet.awaitRefresh()
                        if (result.isError()) {
                            // TODO: Handle non-recoverable errors
                        }
                        wallet.save()
                        delay(10.seconds)
                    }
                }
            }
            mutex.withLock {
                syncedWalletIds.addAll(toSync)
            }
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

        startForeground()

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
            context.startForegroundService(intent)
        }
    }

    private fun startForeground() {
        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(100, notification)
        }
    }

    private fun ensureNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)!!

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Wallet Foreground Service",
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }
}
