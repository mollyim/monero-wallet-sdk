package im.molly.monero.sdk.internal

import android.os.ParcelFileDescriptor
import im.molly.monero.sdk.WalletDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

internal class DataStoreAdapter(
    private val dataStore: WalletDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private val storeName: String? = dataStore::class.simpleName
    private val logger = loggerFor<DataStoreAdapter>()

    suspend fun <T> loadWithFd(
        block: suspend (ParcelFileDescriptor) -> T,
    ): T = withContext(ioDispatcher) {
        val (readFd, writeFd) = ParcelFileDescriptor.createPipe()

        val writerJob = launch {
            FileOutputStream(writeFd.fileDescriptor).use { output ->
                load(output)
            }
        }

        writerJob.invokeOnCompletion {
            writeFd.close()
        }

        try {
            readFd.use { block(readFd) }
        } finally {
            writerJob.join()
        }
    }

    suspend fun <T> saveWithFd(
        overwrite: Boolean,
        block: suspend (ParcelFileDescriptor) -> T,
    ): T = withContext(ioDispatcher) {
        val (readFd, writeFd) = ParcelFileDescriptor.createPipe()

        val readerJob = launch {
            FileInputStream(readFd.fileDescriptor).use { input ->
                save(input, overwrite)
            }
        }

        readerJob.invokeOnCompletion {
            readFd.close()
        }

        try {
            writeFd.use { block(writeFd) }
        } finally {
            readerJob.join()
        }
    }

    private suspend fun load(output: OutputStream) {
        try {
            mutex.withLock {
                dataStore.load().use { input -> input.copyTo(output) }
            }
        } catch (t: Throwable) {
            logger.e("Error loading data from WalletDataStore ($storeName)", t)
            throw t
        }
    }

    private suspend fun save(input: InputStream, overwrite: Boolean) {
        try {
            mutex.withLock {
                dataStore.save(
                    writer = { output -> input.copyTo(output) },
                    overwrite = overwrite,
                )
            }
        } catch (t: Throwable) {
            logger.e("Error saving data to WalletDataStore ($storeName)", t)
            throw t
        }
    }
}
