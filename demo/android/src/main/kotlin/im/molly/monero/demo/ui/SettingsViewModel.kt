package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.RemoteNodeRepository
import im.molly.monero.demo.data.model.RemoteNode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val remoteNodeRepository: RemoteNodeRepository = AppModule.remoteNodeRepository,
) : ViewModel() {

    val remoteNodes: StateFlow<List<RemoteNode>> =
        remoteNodeRepository.getAllRemoteNodes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = listOf(RemoteNode.EMPTY),
            )

    fun remoteNode(remoteNodeId: Long): Flow<RemoteNode> =
        remoteNodeRepository.getRemoteNode(remoteNodeId)

    fun isRemoteNodeCorrect(remoteNode: RemoteNode): Boolean {
        return remoteNode.uri.host?.isEmpty() == false
    }

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
}
