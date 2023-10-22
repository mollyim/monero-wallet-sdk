package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import im.molly.monero.Balance
import im.molly.monero.BlockchainTime
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.common.Result
import im.molly.monero.demo.common.asResult
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.demo.data.model.WalletConfig
import im.molly.monero.demo.data.model.WalletTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WalletViewModel(
    walletId: Long,
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    val uiState: StateFlow<WalletUiState> = walletUiState(
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

        fun key(walletId: Long): String = "wallet_$walletId"
    }

    fun updateConfig(config: WalletConfig) {
        viewModelScope.launch {
            walletRepository.updateWalletConfig(config)
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
                val config = result.data.first
                val ledger = result.data.second
                val balance = ledger.balance
                val blockchainTime = ledger.checkedAt
                val transactions =
                    ledger.transactions
                        .map { WalletTransaction(config.id, it.value) }
                        .sortedByDescending { it.transaction.timestamp }
                WalletUiState.Loaded(config, blockchainTime, balance, transactions)
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
    data class Loaded(
        val config: WalletConfig,
        val blockchainTime: BlockchainTime,
        val balance: Balance,
        val transactions: List<WalletTransaction>,
    ) : WalletUiState

    data object Error : WalletUiState
    data object Loading : WalletUiState
}
