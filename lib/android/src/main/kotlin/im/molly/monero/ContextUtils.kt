package im.molly.monero

import android.app.ActivityManager
import android.app.Application
import android.content.Context.ACTIVITY_SERVICE
import android.os.Build
import android.os.Process

fun Application.isIsolatedProcess(): Boolean {
    return if (Build.VERSION.SDK_INT >= 28) {
        Process.isIsolated()
    } else {
        try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.runningAppProcesses
            false
        } catch (securityException: SecurityException) {
            securityException.message?.contains("isolated", true) ?: false
        } catch (_: Exception) {
            false
        }
    }
}
