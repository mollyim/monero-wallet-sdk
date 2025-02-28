package im.molly.monero.service

import android.content.Context
import im.molly.monero.WalletProvider
import im.molly.monero.internal.WalletServiceClient

/**
 * Provides wallet services using a sandboxed process.
 */
class SandboxedWalletService : BaseWalletService(isolated = true) {
    companion object {
        /**
         * Connects to the sandboxed wallet service and returns a connected [WalletProvider].
         */
        suspend fun connect(context: Context): WalletProvider {
            return WalletServiceClient.bindService(context, SandboxedWalletService::class.java)
        }
    }
}
