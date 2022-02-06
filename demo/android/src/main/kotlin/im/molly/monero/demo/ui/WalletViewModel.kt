package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import im.molly.monero.Ledger
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.common.Result
import im.molly.monero.demo.common.asResult
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.demo.data.model.WalletConfig
import kotlinx.coroutines.flow.*

class WalletViewModel(
    private val walletId: Long,
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    val walletUiState: StateFlow<WalletUiState> = walletUiState(
        walletId = walletId,
        walletRepository = walletRepository,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletUiState.Loading
    )

    companion object {
        fun factory(walletId: Long) = viewModelFactory {
            initializer {
                WalletViewModel(walletId)
            }
        }
    }
}

private fun walletUiState(
    walletId: Long,
    walletRepository: WalletRepository,
): Flow<WalletUiState> {
    return combine(
        walletRepository.getWalletConfig(walletId),
        walletRepository.getLedger(walletId),
        ::Pair,
    ).asResult().map { result ->
        when (result) {
            is Result.Success -> {
                WalletUiState.Success(result.data.first, result.data.second)
            }
            is Result.Loading -> {
                WalletUiState.Loading
            }
            is Result.Error -> {
                WalletUiState.Error
            }
        }
    }
}

sealed interface WalletUiState {
    data class Success(
        val config: WalletConfig,
        val ledger: Ledger,
    ) : WalletUiState

    object Error : WalletUiState
    object Loading : WalletUiState
}
