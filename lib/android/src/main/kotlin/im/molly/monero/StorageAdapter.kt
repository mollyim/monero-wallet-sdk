package im.molly.monero

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class StorageAdapter(var dataStore: WalletDataStore?) : IStorageAdapter.Stub() {

    private val ioStorageScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mutex = Mutex()

    override fun writeAsync(pfd: ParcelFileDescriptor): Boolean {
        val localDataStore = dataStore
        return if (localDataStore != null) {
            val inputStream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
            ioStorageScope.launch {
                mutex.withLock {
                    localDataStore.write { output ->
                        inputStream.copyTo(output)
                    }
                }
            }.invokeOnCompletion { inputStream.close() }
            true
        } else {
            pfd.close()
            false
        }
    }

    override fun readAsync(pfd: ParcelFileDescriptor) {
        val outputStream = ParcelFileDescriptor.AutoCloseOutputStream(pfd)
        ioStorageScope.launch {
            val localDataStore =
                dataStore ?: throw IllegalArgumentException("WalletDataStore cannot be null")
            mutex.withLock {
                localDataStore.read().use { input ->
                    input.copyTo(outputStream)
                }
            }
        }.invokeOnCompletion { outputStream.close() }
    }
}
