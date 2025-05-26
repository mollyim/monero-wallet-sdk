package im.molly.monero.service

import android.content.Context
import im.molly.monero.WalletProvider
import im.molly.monero.internal.WalletServiceClient

/**
 * Provides wallet services using an in-process bound service.
 */
class InProcessWalletService : BaseWalletService() {
    companion object {
        /**
         * Connects to the in-process wallet service and returns a connected [WalletProvider].
         */
        suspend fun connect(context: Context): WalletProvider {
            return WalletServiceClient.bindService(context, InProcessWalletService::class.java)
        }
    }
}
