package im.molly.monero.demo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import im.molly.monero.demo.data.model.SocksProxy
import im.molly.monero.demo.data.model.UserSettings
import im.molly.monero.demo.data.model.toSocketAddress
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val PREF_SOCKS_PROXY = stringPreferencesKey("socks_proxy")
    }

    fun getUserSettings() = dataStore.data.map { prefs ->
        UserSettings(
            socksProxy = prefs[PREF_SOCKS_PROXY]?.let { SocksProxy(it.toSocketAddress()) }
        )
    }

    suspend fun setSocksProxy(socksProxy: SocksProxy?) {
        dataStore.edit { prefs ->
            if (socksProxy != null) {
                prefs[PREF_SOCKS_PROXY] = socksProxy.address().toString()
            } else {
                prefs.remove(PREF_SOCKS_PROXY)
            }
        }
    }
}
