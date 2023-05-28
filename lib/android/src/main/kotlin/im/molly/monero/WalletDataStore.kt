package im.molly.monero

import java.io.InputStream
import java.io.OutputStream

interface WalletDataStore {
    suspend fun write(writer: (OutputStream) -> Unit)
    suspend fun read(): InputStream
}
