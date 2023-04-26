package im.molly.monero.demo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import im.molly.monero.demo.service.SyncService
import im.molly.monero.demo.ui.DemoApp
import im.molly.monero.demo.ui.theme.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val systemUiController = rememberSystemUiController()
            val darkTheme = isSystemInDarkTheme()

            DisposableEffect(systemUiController, darkTheme) {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !darkTheme,
                )

                onDispose {}
            }

            AppTheme(
                darkTheme = darkTheme,
            ) {
                DemoApp()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun connectToSyncService(): SyncService.LocalBinder =
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as SyncService.LocalBinder
                    continuation.resume(binder) {
                        unbindService(this)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                }
            }

            Intent(this@MainActivity, SyncService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
}
