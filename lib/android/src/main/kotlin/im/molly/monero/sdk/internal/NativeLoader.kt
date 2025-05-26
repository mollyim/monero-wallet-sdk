package im.molly.monero.sdk.internal

import java.util.concurrent.atomic.AtomicBoolean

internal object NativeLoader {
    private val wallet = AtomicBoolean()
    private val mnemonics = AtomicBoolean()

    fun loadWalletLibrary(logger: Logger) {
        if (wallet.getAndSet(true)) {
            return
        }
        System.loadLibrary("monero_wallet")
        nativeSetLogger(logger)
    }

    fun loadMnemonicsLibrary() {
        if (mnemonics.getAndSet(true)) {
            return
        }
        System.loadLibrary("monero_mnemonics")
    }
}

private external fun nativeSetLogger(logger: Logger)
