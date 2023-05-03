package im.molly.monero

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class StorageAdapter(var dataStore: WalletDataStore?) : IStorageAdapter.Stub() {

    private val logger = loggerFor<StorageAdapter>()

    private val storageScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mutex = Mutex()

    override fun writeAsync(pfd: ParcelFileDescriptor?): Boolean {
        requireNotNull(pfd)
        val inputStream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        val localDataStore = dataStore
        if (localDataStore == null) {
            logger.i("Unable to save wallet data because WalletDataStore is unset")
            inputStream.close()
            return false
        }
        storageScope.launch {
            mutex.withLock {
                localDataStore.write { output ->
                    inputStream.copyTo(output)
                }
            }
        }.invokeOnCompletion { inputStream.close() }
        return true
    }

    override fun readAsync(pfd: ParcelFileDescriptor?) {
        requireNotNull(pfd)
        val outputStream = ParcelFileDescriptor.AutoCloseOutputStream(pfd)
        val localDataStore = dataStore
        if (localDataStore == null) {
            outputStream.close()
            throw IllegalArgumentException("WalletDataStore cannot be null")
        }
        storageScope.launch {
            mutex.withLock {
                localDataStore.read().use { input ->
                    input.copyTo(outputStream)
                }
            }
        }.invokeOnCompletion { outputStream.close() }
    }
}
