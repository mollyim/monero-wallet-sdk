package im.molly.monero.demo.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.molly.monero.MoneroNetwork
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.RemoteNodeRepository
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.demo.data.model.DefaultMoneroNetwork
import im.molly.monero.demo.data.model.RemoteNode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AddWalletViewModel(
    private val remoteNodeRepository: RemoteNodeRepository = AppModule.remoteNodeRepository,
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    var network by mutableStateOf(DefaultMoneroNetwork)
        private set

    var walletName by mutableStateOf("")
        private set

    val currentRemoteNodes: StateFlow<List<RemoteNode>> =
        snapshotFlow { network }
            .flatMapLatest {
                remoteNodeRepository.getAllRemoteNodes(
                    filterNetworkIds = setOf(network.id),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = WhileSubscribed(5000),
                initialValue = listOf(RemoteNode.EMPTY),
            )

    val selectedRemoteNodes = mutableStateMapOf<Long?, Boolean>()

    private fun getSelectedRemoteNodeIds() =
        selectedRemoteNodes.filterValues { checked -> checked }.keys.filterNotNull()

    fun toggleSelectedNetwork(network: MoneroNetwork) {
        this.network = network
    }

    fun updateWalletName(name: String) {
        this.walletName = name
    }

    fun createWallet() = viewModelScope.launch {
        walletRepository.addWallet(network, walletName, getSelectedRemoteNodeIds())
    }
}
