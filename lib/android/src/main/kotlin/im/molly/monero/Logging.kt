package im.molly.monero

import android.util.Log
import im.molly.monero.internal.Logger

/**
 * Adapter to output logs to the host application.
 *
 * Priority values matches Android framework log priority levels.
 */
interface LogAdapter {
    fun isLoggable(priority: Int, tag: String): Boolean = true
    fun print(priority: Int, tag: String, msg: String?, tr: Throwable?)
}

/**
 * Debug adapter outputs logs to system logger only in debug builds.
 */
class DebugLogAdapter : LogAdapter {
    override fun isLoggable(priority: Int, tag: String): Boolean {
        return priority == Log.ASSERT || BuildConfig.DEBUG
    }

    override fun print(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        when (priority) {
            Log.VERBOSE -> Log.v(tag, msg, tr)
            Log.DEBUG -> Log.d(tag, msg, tr)
            Log.INFO -> Log.i(tag, msg, tr)
            Log.WARN -> Log.w(tag, msg, tr)
            Log.ERROR -> Log.e(tag, msg, tr)
            Log.ASSERT -> Log.wtf(tag, msg, tr)
        }
    }
}

/**
 * Specifies the log adapter to use across the library.
 *
 * By default, the log adapter is [DebugLogAdapter].
 */
fun setLoggingAdapter(logAdapter: LogAdapter) {
    Logger.adapter = logAdapter
}
