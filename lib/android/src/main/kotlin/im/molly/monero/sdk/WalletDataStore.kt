package im.molly.monero.sdk

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface WalletDataStore {
    @Throws(IOException::class)
    suspend fun load(): InputStream

    @Throws(IOException::class)
    suspend fun save(writer: (OutputStream) -> Unit, overwrite: Boolean)
}

class InMemoryWalletDataStore() : WalletDataStore {
    private val data = ByteArrayOutputStream()

    constructor(byteArray: ByteArray) : this() {
        data.write(byteArray)
    }

    override suspend fun load(): InputStream {
        return ByteArrayInputStream(data.toByteArray())
    }

    override suspend fun save(writer: (OutputStream) -> Unit, overwrite: Boolean) {
        check(overwrite || data.size() == 0) { "Wallet data already exists" }
        data.reset()
        writer(data)
    }

    fun toByteArray(): ByteArray {
        return data.toByteArray()
    }
}

fun InMemoryWalletDataStore.copy() = InMemoryWalletDataStore(this.toByteArray())
