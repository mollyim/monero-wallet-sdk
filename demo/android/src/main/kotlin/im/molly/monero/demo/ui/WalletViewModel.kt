package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.common.Result
import im.molly.monero.demo.common.asResult
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.demo.data.model.WalletAddress
import im.molly.monero.demo.data.model.WalletConfig
import im.molly.monero.demo.data.model.WalletTransaction
import im.molly.monero.sdk.Balance
import im.molly.monero.sdk.BlockchainTime
import im.molly.monero.sdk.MoneroNetwork
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

class WalletViewModel(
    private val walletId: Long,
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

    fun createAccount() {
        viewModelScope.launch {
            walletRepository.getWallet(walletId).createAccount()
        }
    }

    fun createSubAddress(accountIndex: Int) {
        viewModelScope.launch {
            walletRepository.getWallet(walletId).createSubAddressForAccount(accountIndex)
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
                val accountBalance = List(ledger.indexedAccounts.size) { index ->
                    index to ledger.getBalanceForAccount(index)
                }.toMap()
                val addresses =
                    ledger.indexedAccounts.flatMap { account ->
                        account.addresses.map { address ->
                            WalletAddress(
                                address = address,
                                enotes = ledger.enoteSet.filter { it.value.owner == address },
                                used = address.isAddressUsed(ledger.transactions),
                                isLastForAccount = address === account.addresses.last(),
                            )
                        }
                    }
                val transactions =
                    ledger.transactions.map { WalletTransaction(config.id, it) }
                        .sortedByDescending { it.transaction.blockTimestamp ?: Instant.MAX }
                WalletUiState.Loaded(
                    config = config,
                    network = ledger.publicAddress.network,
                    blockchainTime = ledger.checkedAt,
                    totalBalance = ledger.getBalance(),
                    accountBalance = accountBalance,
                    addresses = addresses,
                    transactions = transactions,
                )
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
        val network: MoneroNetwork,
        val blockchainTime: BlockchainTime,
        val totalBalance: Balance,
        val accountBalance: Map<Int, Balance>,
        val addresses: List<WalletAddress>,
        val transactions: List<WalletTransaction>,
    ) : WalletUiState

    data object Error : WalletUiState
    data object Loading : WalletUiState
}
