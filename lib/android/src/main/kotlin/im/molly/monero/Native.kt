package im.molly.monero

import java.util.concurrent.atomic.AtomicBoolean

internal object MoneroJni {
    private val initialized = AtomicBoolean()

    fun loadLibrary(logger: Logger) {
        if (initialized.getAndSet(true)) {
            return
        }
        System.loadLibrary("monero_jni")
        nativeSetLogger(logger)
    }
}

private external fun nativeSetLogger(logger: Logger)
