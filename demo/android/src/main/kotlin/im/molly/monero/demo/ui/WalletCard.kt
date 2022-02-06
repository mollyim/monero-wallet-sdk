package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun WalletCard(
    walletId: Long,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.factory(walletId),
        key = walletId.toString(),
    ),
) {
    val walletUiState: WalletUiState by viewModel.walletUiState.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .padding(8.dp)
    ) {
        Column {
            when (walletUiState) {
                WalletUiState.Error -> {
                    Text(text = "Error") // TODO
                }
                WalletUiState.Loading -> {
                    Text(text = "Loading wallet...") // TODO
                }
                is WalletUiState.Success -> {
                    val state = walletUiState as WalletUiState.Success
                    Row {
                        Text(text = "Name: ${state.config.name}")
                    }
                    Row {
                        Text(text = "Ledger: ${state.ledger}")
                    }
                }
            }
        }
    }
}
