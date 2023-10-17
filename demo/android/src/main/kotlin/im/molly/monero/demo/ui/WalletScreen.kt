package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.BlockchainTime
import im.molly.monero.Ledger
import im.molly.monero.MoneroCurrency
import im.molly.monero.demo.data.model.WalletConfig
import im.molly.monero.demo.ui.component.Toolbar
import im.molly.monero.demo.ui.theme.AppIcons
import im.molly.monero.demo.ui.theme.AppTheme

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
        uiState = walletUiState,
        onWalletConfigChange = { config -> viewModel.updateConfig(config) },
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun WalletScreen(
    uiState: WalletUiState,
    modifier: Modifier = Modifier,
    onWalletConfigChange: (WalletConfig) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    when (uiState) {
        WalletUiState.Error -> WalletScreenError(onBackClick = onBackClick)
        WalletUiState.Loading -> WalletScreenLoading(onBackClick = onBackClick)
        is WalletUiState.Success -> WalletScreenPopulated(
            walletConfig = uiState.config,
            ledger = uiState.ledger,
            onWalletConfigChange = onWalletConfigChange,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun WalletScreenError(
    onBackClick: () -> Unit,
) {
}

@Composable
private fun WalletScreenLoading(
    onBackClick: () -> Unit,
) {
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletScreenPopulated(
    walletConfig: WalletConfig,
    ledger: Ledger,
    onWalletConfigChange: (WalletConfig) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    WalletKebabMenu(
                        onRenameClick = { showRenameDialog = true },
                        onDeleteClick = { },
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                style = MaterialTheme.typography.headlineLarge,
                text = buildAnnotatedString {
                    append(MoneroCurrency.SYMBOL + " ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(MoneroCurrency.Formatter(precision = 5).format(ledger.balance.confirmedAmount))
                    }
                }
            )
            Text(text = walletConfig.name, style = MaterialTheme.typography.headlineSmall)
        }

        if (showRenameDialog) {
            var name by remember { mutableStateOf(walletConfig.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Enter wallet name") },
                text = {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onWalletConfigChange(walletConfig.copy(name = name))
                        showRenameDialog = false
                    }) {
                        Text("Rename")
                    }
                },
            )
        }
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
            contentDescription = "Open menu",
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = {
                onRenameClick()
                expanded = false
            },
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDeleteClick()
                expanded = false
            },
        )
    }
}

@Preview
@Composable
private fun WalletScreenPreview() {
    AppTheme {
        WalletScreen(
            uiState = WalletUiState.Success(
                WalletConfig(
                    id = 0,
                    publicAddress = "888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H",
                    filename = "",
                    name = "Personal",
                    remoteNodes = emptySet(),
                ),
                Ledger(
                    publicAddress = "888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H",
                    transactions = emptyMap(),
                    enotes = emptySet(),
                    checkedAt = BlockchainTime.Genesis,
                ),
            ),
        )
    }
}
