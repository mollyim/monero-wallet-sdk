package im.molly.monero.demo.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.demo.R
import im.molly.monero.demo.ui.component.Toolbar
import im.molly.monero.demo.ui.theme.AppIcons

@Composable
fun WalletRoute(
    walletId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.factory(walletId),
        key = walletId.toString(),
    )
) {
    val walletUiState: WalletUiState by viewModel.walletUiState.collectAsStateWithLifecycle()

    WalletScreen(
        walletUiState = walletUiState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletScreen(
    walletUiState: WalletUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    WalletKebabMenu({}, {})
                }
            )
        }
    ) { padding ->
    }
}

@Composable
private fun WalletKebabMenu(
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = AppIcons.MoreVert,
            contentDescription = stringResource(R.string.open_menu),
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.rename)) },
            onClick = {
                onRenameClick()
                expanded = false
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.delete)) },
            onClick = {
                onDeleteClick()
                expanded = false
            },
        )
    }
}
