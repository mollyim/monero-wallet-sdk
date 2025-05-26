package im.molly.monero.sdk.service

import android.content.Context
import im.molly.monero.sdk.WalletProvider
import im.molly.monero.sdk.internal.WalletServiceClient

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
