package im.molly.monero.sdk.internal

internal object WalletServiceLogListener : IWalletServiceListener.Stub() {
    override fun onLogMessage(priority: Int, tag: String, msg: String, cause: String?) {
        if (Logger.adapter.isLoggable(priority, tag)) {
            val tr = if (cause != null) Throwable(cause) else null
            Logger.adapter.print(priority, tag, msg, tr)
        }
    }
}