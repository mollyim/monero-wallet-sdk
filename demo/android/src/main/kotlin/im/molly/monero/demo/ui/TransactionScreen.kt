package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.MoneroCurrency
import im.molly.monero.Transaction
import im.molly.monero.demo.ui.component.Toolbar
import im.molly.monero.demo.ui.preview.PreviewParameterData
import im.molly.monero.demo.ui.theme.AppIcons
import im.molly.monero.demo.ui.theme.AppTheme


@Composable
fun TransactionRoute(
    txId: String,
    walletId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModel.factory(txId, walletId),
        key = TransactionViewModel.key(txId, walletId),
    )
) {
    val uiState: TxUiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionScreen(
    uiState: TxUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    Scaffold(topBar = {
        Toolbar(
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            title = "Transaction details",
        )
    }) { padding ->
        Box(
            modifier = modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(10.dp)) {
                when (uiState) {
                    is TxUiState.Loaded -> {
                        val tx = uiState.transaction
                        TransactionDetail("State", tx.state.toString())
                        Spacer(modifier = Modifier.height(12.dp))
                        TransactionDetail("Sent", tx.sent.toString())
                        Spacer(modifier = Modifier.height(12.dp))
                        TransactionDetail("Received", tx.received.toString())
                        Spacer(modifier = Modifier.height(12.dp))
                        TransactionDetail("Payments", tx.payments.toString())
                        Spacer(modifier = Modifier.height(12.dp))
                        TransactionDetail("Time lock", tx.timeLock.toString())
                        Spacer(modifier = Modifier.height(12.dp))
                        TransactionDetail("Change", MoneroCurrency.format(tx.change))
                        Spacer(modifier = Modifier.height(12.dp))
                        TransactionDetail("Fee", MoneroCurrency.format(tx.fee))
                        Spacer(modifier = Modifier.height(12.dp))
                        TransactionDetail("Transaction ID", tx.txId)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun TransactionDetail(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column() {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = modifier)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}

@Preview
@Composable
private fun TransactionScreenPopulated(
    @PreviewParameter(
        TransactionScreenPreviewParameterProvider::class,
        limit = 1,
    ) transactions: List<Transaction>,
) {
    AppTheme {
        TransactionScreen(
            uiState = TxUiState.Loaded(transactions.first()),
            onBackClick = {},
        )
    }
}

@Preview
@Composable
private fun TransactionScreenError() {
    AppTheme {
        TransactionScreen(
            uiState = TxUiState.Error,
            onBackClick = {},
        )
    }
}

@Preview
@Composable
private fun TransactionScreenNotFound() {
    AppTheme {
        TransactionScreen(
            uiState = TxUiState.NotFound,
            onBackClick = {},
        )
    }
}

class TransactionScreenPreviewParameterProvider : PreviewParameterProvider<List<Transaction>> {
    override val values = sequenceOf(PreviewParameterData.transactions)
}
