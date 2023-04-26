package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.molly.monero.demo.AppModule

import im.molly.monero.demo.data.RemoteNodeRepository
import im.molly.monero.demo.data.SettingsRepository
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.demo.data.model.SocksProxy
import im.molly.monero.demo.data.model.toSocketAddress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.Proxy

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = AppModule.settingsRepository,
    private val remoteNodeRepository: RemoteNodeRepository = AppModule.remoteNodeRepository,
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    val settingsUiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.getUserSettings(),
            remoteNodeRepository.getAllRemoteNodes(),
            ::Pair,
        ).map {
            SettingsUiState.Success(
                socksProxyAddress = it.first.socksProxy?.address()?.toString().orEmpty(),
                remoteNodes = it.second,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Loading,
        )

    fun remoteNode(remoteNodeId: Long): Flow<RemoteNode> =
        remoteNodeRepository.getRemoteNode(remoteNodeId)

    fun isRemoteNodeCorrect(remoteNode: RemoteNode): Boolean =
        remoteNode.uri.host?.isEmpty() == false

    fun saveRemoteNodeDetails(remoteNode: RemoteNode) {
        viewModelScope.launch {
            remoteNodeRepository.addOrUpdateRemoteNode(remoteNode)
        }
    }

    fun forgetRemoteNodeDetails(remoteNode: RemoteNode) {
        viewModelScope.launch {
            remoteNodeRepository.deleteRemoteNode(remoteNode)
        }
    }

    fun isSocksProxyAddressCorrect(address: String) =
        try {
            if (address.isNotEmpty()) address.toSocketAddress()
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    fun setSocksProxyAddress(address: String) {
        viewModelScope.launch {
            val socksProxy =
                if (address.isNotEmpty()) SocksProxy(address.toSocketAddress()) else null
            settingsRepository.setSocksProxy(socksProxy)
        }
    }

    init {
        // Consider using a ProxySelector in OkHttpClient instead once this bug is resolved:
        // https://github.com/square/okhttp/issues/7698
        viewModelScope.launch {
            settingsRepository.getUserSettings()
                .distinctUntilChangedBy { it.activeProxy }
                .map { it.activeProxy }
                .collect { onProxyChanged(it) }
        }
    }

    private suspend fun onProxyChanged(newProxy: Proxy) {
        walletRepository.getRemoteClients().first().forEach {
            val currentProxy = it.httpClient.proxy()
            if (currentProxy != newProxy) {
                val builder = it.httpClient.newBuilder()
                it.httpClient = builder.proxy(newProxy).build()
            }
        }
    }
}

sealed interface SettingsUiState {
    data class Success(
        val socksProxyAddress: String,
        val remoteNodes: List<RemoteNode>,
    ) : SettingsUiState

    object Loading : SettingsUiState
}
