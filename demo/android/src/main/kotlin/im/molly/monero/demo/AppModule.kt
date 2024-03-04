package im.molly.monero.demo

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import im.molly.monero.demo.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Naive container of global instances.
 *
 * A complex app should use Koin or Hilt for dependencies.
 */
object AppModule {
    private lateinit var application: Application
    private lateinit var populateInitialData: suspend (AppDatabase) -> Unit

    private val applicationScope = kotlinx.coroutines.MainScope()

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application, AppDatabase::class.java, "monero-demo.db"
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                applicationScope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }).build()
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

    fun provide(application: Application, populateInitialData: suspend (AppDatabase) -> Unit) {
        this.application = application
        this.populateInitialData = populateInitialData
    }
}
