package im.molly.monero.sdk.internal

import android.util.Log
import im.molly.monero.sdk.DebugLogAdapter
import im.molly.monero.sdk.LogAdapter

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
