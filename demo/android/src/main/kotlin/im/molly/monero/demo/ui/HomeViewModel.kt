package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.WalletRepository
import kotlinx.coroutines.flow.*

class HomeViewModel(
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    val walletListUiState: StateFlow<WalletListUiState> = walletRepository.getWalletIdList()
        .map { WalletListUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WalletListUiState.Loading,
        )

}

sealed interface HomeUiState {
    data object Loading : HomeUiState

//    data class Ready(
//        val wallets: List<WalletDetails>
//    ) : HomeUiState

    data object Empty : HomeUiState
}

sealed interface WalletListUiState {
    data class Success(val ids: List<Long>) : WalletListUiState
    data object Loading : WalletListUiState
}
