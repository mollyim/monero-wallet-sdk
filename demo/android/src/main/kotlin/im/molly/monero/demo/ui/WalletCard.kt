package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletCard(
    walletId: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.factory(walletId),
        key = WalletViewModel.key(walletId),
    ),
) {
    val uiState: WalletUiState by viewModel.uiState.collectAsStateWithLifecycle()

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            when (uiState) {
                is WalletUiState.Loaded -> WalletCardExpanded(uiState as WalletUiState.Loaded)
                WalletUiState.Error -> WalletCardError()
                WalletUiState.Loading -> WalletCardLoading()
            }
        }
    }
}

@Composable
fun WalletCardExpanded(
    uiState: WalletUiState.Loaded,
) {
    Row {
        Text(
            text = "Wallet ID #${uiState.config.id} (${uiState.config.name}) ${uiState.network.name}",
            style = MaterialTheme.typography.bodyMedium,
            )
    }
    Row {
        Text(
            text = uiState.config.publicAddress,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Row {
        Text(
            text = uiState.blockchainTime.toString(),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun WalletCardError() {
    Text(text = "Error") // TODO
}

@Composable
fun WalletCardLoading() {
    Text(text = "Loading wallet...") // TODO
}

//@Preview
//@Composable
//private fun WalletCardExpandedPreview() {
//    AppTheme {
//        Surface {
//            WalletCardExpanded()
//        }
//    }
//}
