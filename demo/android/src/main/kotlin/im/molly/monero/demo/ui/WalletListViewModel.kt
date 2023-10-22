package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.WalletRepository
import kotlinx.coroutines.flow.*

class WalletListViewModel(
    walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    val uiState: StateFlow<WalletListUiState> =
        walletRepository.getWalletIdList().map { walletIds ->
            if (walletIds.isNotEmpty()) {
                WalletListUiState.Loaded(walletIds)
            } else {
                WalletListUiState.Empty
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WalletListUiState.Loading,
        )
}

sealed interface WalletListUiState {
    data class Loaded(val walletIds: List<Long>) : WalletListUiState
    data object Loading : WalletListUiState
    data object Empty : WalletListUiState
}
