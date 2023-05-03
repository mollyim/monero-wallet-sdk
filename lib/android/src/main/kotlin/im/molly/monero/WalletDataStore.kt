package im.molly.monero

import java.io.FileInputStream
import java.io.FileOutputStream

interface WalletDataStore {
    suspend fun write(writer: (FileOutputStream) -> Unit)
    suspend fun read(): FileInputStream
}
