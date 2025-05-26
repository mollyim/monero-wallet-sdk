package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.common.Result
import im.molly.monero.demo.common.asResult
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.sdk.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TransactionViewModel(
    txId: String,
    walletId: Long,
    walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    val uiState: StateFlow<TxUiState> =
        walletRepository.getTransaction(walletId, txId)
            .asResult()
            .map { result ->
                when (result) {
                    is Result.Success -> result.data?.let { tx ->
                        TxUiState.Loaded(tx)
                    } ?: TxUiState.NotFound

                    is Result.Error -> TxUiState.Error
                    is Result.Loading -> TxUiState.Loading
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TxUiState.Loading
            )

    companion object {
        fun factory(txId: String, walletId: Long) = viewModelFactory {
            initializer {
                TransactionViewModel(txId, walletId)
            }
        }

        fun key(txId: String, walletId: Long): String = "tx_$txId:$walletId"
    }
}

sealed interface TxUiState {
    data class Loaded(val transaction: Transaction) : TxUiState
    data object Error : TxUiState
    data object Loading : TxUiState
    data object NotFound : TxUiState
}
