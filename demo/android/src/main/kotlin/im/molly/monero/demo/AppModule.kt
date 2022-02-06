package im.molly.monero.demo

import android.app.Application
import androidx.room.Room
import im.molly.monero.demo.data.*
import okhttp3.OkHttpClient

/**
 * Naive container of global instances.
 *
 * A complex app should use Koin or Hilt for dependencies.
 */
object AppModule {
    lateinit var application: Application

    private val applicationScope = kotlinx.coroutines.MainScope()

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application, AppDatabase::class.java, "monero-demo.db"
        ).build()
    }

    private val walletHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    private val walletDataSource: WalletDataSource by lazy {
        WalletDataSource(database.walletDao())
    }

    private val walletDataFileStorage: WalletDataFileStorage by lazy {
        AppWalletDataFileStorage(application)
    }

    private val moneroSdkClient: MoneroSdkClient by lazy {
        MoneroSdkClient(application, walletDataFileStorage, walletHttpClient)
    }

    val remoteNodeRepository: RemoteNodeRepository by lazy {
        RemoteNodeRepository(database.remoteNodeDao())
    }

    val walletRepository: WalletRepository by lazy {
        WalletRepository(moneroSdkClient, walletDataSource, applicationScope)
    }

    fun provide(application: Application) {
        this.application = application
    }
}
