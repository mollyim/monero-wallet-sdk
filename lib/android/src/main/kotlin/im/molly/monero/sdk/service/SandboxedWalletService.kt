package im.molly.monero.sdk.service

import android.content.Context
import im.molly.monero.sdk.WalletProvider
import im.molly.monero.sdk.internal.WalletServiceClient

/**
 * Provides wallet services using a sandboxed process.
 */
class SandboxedWalletService : BaseWalletService() {
    companion object {
        /**
         * Connects to the sandboxed wallet service and returns a connected [WalletProvider].
         */
        suspend fun connect(context: Context): WalletProvider {
            return WalletServiceClient.bindService(context, SandboxedWalletService::class.java)
        }
    }
}
