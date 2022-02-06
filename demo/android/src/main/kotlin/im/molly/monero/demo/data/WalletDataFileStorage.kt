package im.molly.monero.demo.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

interface WalletDataFileStorage {
    fun tryWriteData(
        publicAddress: String,
        canOverwrite: Boolean = true,
        block: (FileOutputStream) -> Unit,
    )

    fun readData(publicAddress: String): FileInputStream
}

class AppWalletDataFileStorage(context: Context) : WalletDataFileStorage {
    private val filesDir = context.filesDir

    override fun tryWriteData(
        publicAddress: String,
        canOverwrite: Boolean,
        block: (FileOutputStream) -> Unit,
    ) {
        val file = getBackingFile(publicAddress)
        if (!(canOverwrite || file.baseFile.createNewFile())) {
            throw IOException("Data file already exists: ${file.baseFile.path}")
        }
        val output = file.startWrite()
        try {
            block(output)
            file.finishWrite(output)
        } catch (ioe: IOException) {
            file.failWrite(output)
            throw ioe
        }
    }

    override fun readData(publicAddress: String): FileInputStream {
        val file = getBackingFile(publicAddress)
        return file.openRead()
    }

    private fun getBackingFile(publicAddress: String): AtomicFile {
        val uniqueFilename = publicAddress.substring(0, 11) + ".wallet"
        return AtomicFile(File(getOrCreateWalletDataDir(), uniqueFilename))
    }

    private fun getOrCreateWalletDataDir(): File {
        val walletDataDir = File(filesDir, "wallet_data")
        if (walletDataDir.exists() || walletDataDir.mkdir()) {
            return walletDataDir
        }
        throw IOException("Cannot create wallet data directory: ${walletDataDir.path}")
    }
}
