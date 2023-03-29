package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.demo.R
import im.molly.monero.demo.ui.component.Toolbar

@Composable
fun HomeRoute(
    navigateToAddWalletWizard: () -> Unit,
    navigateToWallet: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val walletListUiState: WalletListUiState by viewModel.walletListUiState.collectAsStateWithLifecycle()

    HomeScreen(
        walletListUiState = walletListUiState,
        onAddWalletClick = navigateToAddWalletWizard,
        onWalletClick = navigateToWallet,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    walletListUiState: WalletListUiState,
    onAddWalletClick: () -> Unit,
    onWalletClick: (Long) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Toolbar(
                title = stringResource(R.string.monero_wallets),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddWalletClick) {
                Text(stringResource(R.string.add_wallet))
            }
        },
    ) { padding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding))
        {
            walletCards(walletListUiState, onWalletClick)
        }
    }
}

private fun LazyListScope.walletCards(
    walletListUiState: WalletListUiState,
    onWalletClick: (Long) -> Unit,
) {
    when (walletListUiState) {
        WalletListUiState.Loading -> item {
            Text(text = "Loading wallet list...") // TODO
        }
        is WalletListUiState.Success -> {
            walletCardsItems(walletListUiState.ids, onWalletClick)
        }
    }
}
