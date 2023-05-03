package im.molly.monero.demo

import android.app.Application
import androidx.room.Room
import im.molly.monero.demo.data.*

/**
 * Naive container of global instances.
 *
 * A complex app should use Koin or Hilt for dependencies.
 */
object AppModule {
    private lateinit var application: Application

    private val applicationScope = kotlinx.coroutines.MainScope()

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application, AppDatabase::class.java, "monero-demo.db"
        ).build()
    }

    private val walletDataSource: WalletDataSource by lazy {
        WalletDataSource(database.walletDao())
    }

    private val moneroSdkClient: MoneroSdkClient by lazy {
        MoneroSdkClient(application)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(application.preferencesDataStore)
    }

    val remoteNodeRepository: RemoteNodeRepository by lazy {
        RemoteNodeRepository(database.remoteNodeDao())
    }

    val walletRepository: WalletRepository by lazy {
        WalletRepository(moneroSdkClient, walletDataSource, settingsRepository, applicationScope)
    }

    fun provide(application: Application) {
        this.application = application
    }
}
