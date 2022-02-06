package im.molly.monero.demo

import android.app.Application
import android.os.StrictMode
import im.molly.monero.demo.service.SyncService
import im.molly.monero.isIsolatedProcess

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
        AppModule.provide(this)
        SyncService.start(this)
    }
}
