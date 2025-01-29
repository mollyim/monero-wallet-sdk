package im.molly.monero

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

interface WalletDataStore {
    suspend fun write(writer: (OutputStream) -> Unit)
    suspend fun read(): InputStream
}

class InMemoryWalletDataStore : WalletDataStore {
    private val data = ByteArrayOutputStream()

    override suspend fun write(writer: (OutputStream) -> Unit) {
        data.reset()
        writer(data)
    }

    override suspend fun read(): InputStream {
        return ByteArrayInputStream(data.toByteArray())
    }
}
