package im.molly.monero.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import im.molly.monero.demo.AppModule
import im.molly.monero.demo.data.WalletRepository
import im.molly.monero.sdk.FeePriority
import im.molly.monero.sdk.MoneroCurrency
import im.molly.monero.sdk.PaymentDetail
import im.molly.monero.sdk.PaymentRequest
import im.molly.monero.sdk.PendingTransfer
import im.molly.monero.sdk.PublicAddress
import im.molly.monero.sdk.TransferRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SendTabViewModel(
    private val walletId: Long,
    private val walletRepository: WalletRepository = AppModule.walletRepository,
) : ViewModel() {

    private val viewModelState = MutableStateFlow(SendTabUiState())

    val uiState = viewModelState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = viewModelState.value
    )

    fun updateAccount(accountIndex: Int) {
        viewModelState.update {
            it.copy(
                accountIndex = accountIndex,
                status = TransferStatus.Idle,
            )
        }
    }

    fun updatePriority(priority: FeePriority) {
        viewModelState.update {
            it.copy(
                feePriority = priority,
                status = TransferStatus.Idle,
            )
        }
    }

    fun updateRecipients(recipients: List<Pair<String, String>>) {
        viewModelState.update {
            it.copy(
                recipients = recipients, status = TransferStatus.Idle
            )
        }
    }

    private fun getPaymentRequest(state: SendTabUiState): Result<PaymentRequest> {
        return runCatching {
            PaymentRequest(
                spendingAccountIndex = state.accountIndex,
                paymentDetails = state.recipients.map { (address, amount) ->
                    PaymentDetail(
                        recipientAddress = PublicAddress.parse(address),
                        amount = MoneroCurrency.parse(amount),
                    )
                },
                feePriority = state.feePriority,
            )
        }
    }

    fun createPayment() {
        val result = getPaymentRequest(viewModelState.value)
        result.fold(
            onSuccess = { createTransfer(it) },
            onFailure = { error ->
                viewModelState.update {
                    it.copy(status = TransferStatus.Error(error.message))
                }
            },
        )
    }

    private fun createTransfer(transferRequest: TransferRequest) {
        viewModelState.update {
            it.copy(status = TransferStatus.Preparing)
        }
        viewModelScope.launch {
            val pendingTransfer = runCatching {
                walletRepository.createTransfer(walletId, transferRequest)
            }
            viewModelState.update {
                val updatedState = pendingTransfer.fold(
                    onSuccess = { pendingTransfer ->
                        it.copy(status = TransferStatus.ReadyForApproval(pendingTransfer))
                    },
                    onFailure = { error ->
                        it.copy(status = TransferStatus.Error(error.message))
                    },
                )
                updatedState
            }
        }
    }

    fun confirmPendingTransfer() {
        val savedState = viewModelState.getAndUpdate {
            it.copy(status = TransferStatus.Sending)
        }
        check(savedState.status is TransferStatus.ReadyForApproval)
        viewModelScope.launch {
            val result = runCatching {
                savedState.status.pendingTransfer.use {
                    it.commit()
                }
            }
            viewModelState.update {
                val updatedState = result.fold(
                    onSuccess = { success ->
                        it.copy(
                            recipients = emptyList(),
                            status = TransferStatus.Sent,
                        )
                    },
                    onFailure = { error ->
                        it.copy(status = TransferStatus.Error(error.message))
                    },
                )
                updatedState
            }
        }
    }

    fun cancelPendingTransfer() {
        val savedState = viewModelState.getAndUpdate {
            it.copy(status = TransferStatus.Idle)
        }
        if (savedState.status is TransferStatus.ReadyForApproval) {
            savedState.status.pendingTransfer.close()
        }
    }

    companion object {
        fun factory(walletId: Long) = viewModelFactory {
            initializer {
                SendTabViewModel(walletId)
            }
        }
    }
}

data class SendTabUiState(
    val accountIndex: Int = 0,
    val recipients: List<Pair<String, String>> = emptyList(),
    val feePriority: FeePriority = FeePriority.Medium,
    val status: TransferStatus = TransferStatus.Idle,
) {
    val isInProgress: Boolean
        get() = status == TransferStatus.Preparing ||
                status == TransferStatus.Sending ||
                status is TransferStatus.ReadyForApproval
}

sealed interface TransferStatus {
    data object Idle : TransferStatus
    data object Preparing : TransferStatus
    data object Sending : TransferStatus
    data object Sent : TransferStatus
    data class ReadyForApproval(
        val pendingTransfer: PendingTransfer
    ) : TransferStatus

    data class Error(val errorMessage: String?) : TransferStatus
}
