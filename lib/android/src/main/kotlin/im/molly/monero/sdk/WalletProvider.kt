package im.molly.monero.sdk

import java.io.Closeable

interface WalletProvider : Closeable {
    suspend fun createNewWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore? = null,
        client: MoneroNodeClient? = null,
    ): MoneroWallet

    suspend fun restoreWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore? = null,
        client: MoneroNodeClient? = null,
        secretSpendKey: SecretKey,
        restorePoint: RestorePoint,
    ): MoneroWallet

    suspend fun openWallet(
        network: MoneroNetwork,
        dataStore: WalletDataStore,
        client: MoneroNodeClient? = null,
    ): MoneroWallet

    fun isServiceSandboxed(): Boolean

    fun disconnect()

    override fun close() {
        disconnect()
    }

    /** Exception thrown if the wallet service cannot be bound. */
    class ServiceNotBoundException : Exception()
}
