package im.molly.monero.demo.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.molly.monero.MoneroNetwork
import im.molly.monero.RestorePoint
import im.molly.monero.SecretKey
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.RemoteNodeRepository
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.demo.data.model.DefaultMoneroNetwork
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.mnemonics.MoneroMnemonic
import im.molly.monero.util.parseHex
import im.molly.monero.util.toHex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddWalletViewModel(
    private val remoteNodeRepository: RemoteNodeRepository = AppModule.remoteNodeRepository,
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    private val viewModelState = MutableStateFlow(AddWalletUiState())

    val uiState = viewModelState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = viewModelState.value
    )

    val currentRemoteNodes: StateFlow<List<RemoteNode>> =
        uiState.flatMapLatest {
            remoteNodeRepository.getAllRemoteNodes(
                filterNetworkIds = setOf(it.network.id),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val selectedRemoteNodes = mutableStateMapOf<Long?, Boolean>()

    private fun getSelectedRemoteNodeIds() =
        selectedRemoteNodes.filterValues { checked -> checked }.keys.filterNotNull()

    fun toggleSelectedNetwork(network: MoneroNetwork) {
        viewModelState.update { it.copy(network = network) }
        selectedRemoteNodes.clear()
    }

    fun updateWalletName(name: String) {
        viewModelState.update { it.copy(walletName = name) }
    }

    fun updateSecretSpendKeyHex(value: String) {
        viewModelState.update { it.copy(secretSpendKeyHex = value) }
    }

    fun recoverFromMnemonic(words: String): Boolean {
        MoneroMnemonic.recoverEntropy(words)?.use { mnemonicCode ->
            val secretKey = SecretKey(mnemonicCode.entropy)
            viewModelState.update {
                it.copy(secretSpendKeyHex = secretKey.bytes.toHex())
            }
            secretKey.destroy()
            return true
        }
        return false
    }

    fun updateCreationDate(value: String) {
        viewModelState.update { it.copy(creationDate = value) }
    }

    fun updateRestoreHeight(value: String) {
        viewModelState.update { it.copy(restoreHeight = value) }
    }

    fun validateSecretSpendKeyHex(): Boolean =
        with(viewModelState.value) {
            return secretSpendKeyHex.length == 64 && runCatching {
                secretSpendKeyHex.parseHex()
            }.isSuccess
        }

    fun validateCreationDate(): Boolean =
        with(viewModelState.value) {
            creationDate.isEmpty() || runCatching {
                RestorePoint.creationTime(LocalDate.parse(creationDate))
            }.isSuccess
        }

    fun validateRestoreHeight(): Boolean =
        with(viewModelState.value) {
            restoreHeight.isEmpty() || runCatching {
                RestorePoint.blockHeight(restoreHeight.toInt())
            }.isSuccess
        }

    fun createWallet() {
        val state = viewModelState.getAndUpdate { it.copy(isInProgress = true) }
        viewModelScope.launch {
            walletRepository.addWallet(state.network, state.walletName, getSelectedRemoteNodeIds())
            viewModelState.update { it.copy(walletAdded = true) }
        }
    }

    fun restoreWallet() {
        val state = viewModelState.getAndUpdate {
            it.copy(isInProgress = true)
        }
        viewModelScope.launch {
            val restorePoint = when {
                state.creationDate.isNotEmpty() ->
                    RestorePoint.creationTime(LocalDate.parse(state.creationDate))

                state.restoreHeight.isNotEmpty() ->
                    RestorePoint.blockHeight(state.restoreHeight.toInt())

                else -> RestorePoint.Genesis
            }
            SecretKey(state.secretSpendKeyHex.parseHex()).use { secretSpendKey ->
                walletRepository.restoreWallet(
                    state.network,
                    state.walletName,
                    getSelectedRemoteNodeIds(),
                    secretSpendKey,
                    restorePoint
                )
            }
            viewModelState.update { it.copy(walletAdded = true) }
        }
    }
}

data class AddWalletUiState(
    val network: MoneroNetwork = DefaultMoneroNetwork,
    val walletName: String = "",
    val secretSpendKeyHex: String = "",
    val creationDate: String = "",
    val restoreHeight: String = "",
    val isInProgress: Boolean = false,
    val walletAdded: Boolean = false,
)
