package im.molly.monero.demo

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import im.molly.monero.demo.service.SyncService
import im.molly.monero.isIsolatedProcess

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )
        if (isIsolatedProcess()) {
            return
        }
        AppModule.provide(
            application = this,
            populateInitialData = { db ->
                addDefaultRemoteNodes(db)
            },
        )
        SyncService.start(this)
    }
}
