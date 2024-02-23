package im.molly.monero

import android.util.Log

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

internal open class Logger(val tag: String) : LogAdapter {
    fun v(msg: String? = null, tr: Throwable? = null) = log(Log.VERBOSE, tag, msg, tr)
    fun d(msg: String? = null, tr: Throwable? = null) = log(Log.DEBUG, tag, msg, tr)
    fun i(msg: String? = null, tr: Throwable? = null) = log(Log.INFO, tag, msg, tr)
    fun w(msg: String? = null, tr: Throwable? = null) = log(Log.WARN, tag, msg, tr)
    fun e(msg: String? = null, tr: Throwable? = null) = log(Log.ERROR, tag, msg, tr)
    fun wtf(msg: String? = null, tr: Throwable? = null) = log(Log.ASSERT, tag, msg, tr)

    fun log(priority: Int, tag: String, msg: String?, tr: Throwable? = null) {
        if (isLoggable(priority, tag)) {
            print(priority, tag, msg, tr)
        }
    }

    inline fun v(lazyMsg: () -> String?) = log(Log.VERBOSE, tag, lazyMsg)
    inline fun d(lazyMsg: () -> String?) = log(Log.DEBUG, tag, lazyMsg)
    inline fun i(lazyMsg: () -> String?) = log(Log.INFO, tag, lazyMsg)
    inline fun w(lazyMsg: () -> String?) = log(Log.WARN, tag, lazyMsg)
    inline fun e(lazyMsg: () -> String?) = log(Log.ERROR, tag, lazyMsg)
    inline fun wtf(lazyMsg: () -> String?) = log(Log.ASSERT, tag, lazyMsg)

    inline fun log(priority: Int, tag: String, lazyMsg: () -> String?) {
        if (isLoggable(priority, tag)) {
            print(priority, tag, lazyMsg(), null)
        }
    }

    /**
     * Log method called from native code.
     */
    @CalledByNative
    fun logFromNative(priority: Int, tag: String, msg: String?) {
        val pri = if (priority in Log.VERBOSE.rangeTo(Log.ASSERT)) priority else Log.ASSERT
        val jniTag = "MoneroJNI.$tag"
        log(pri, jniTag, msg, null)
    }

    companion object {
        var adapter: LogAdapter = DebugLogAdapter()
    }

    override fun print(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        adapter.print(priority, tag, msg, tr)
    }
}

internal inline fun <reified T : Any> loggerFor(): Logger = Logger(getTag(T::class.java))

private fun getTag(clazz: Class<*>): String {
    val tag = clazz.simpleName
    return if (tag.length <= 23) {
        tag
    } else {
        tag.substring(0, 23)
    }
}
