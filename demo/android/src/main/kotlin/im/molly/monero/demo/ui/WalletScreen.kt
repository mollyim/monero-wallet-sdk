package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.Ledger
import im.molly.monero.MoneroCurrency
import im.molly.monero.PublicAddress
import im.molly.monero.demo.data.model.WalletConfig
import im.molly.monero.demo.ui.component.Toolbar
import im.molly.monero.demo.ui.preview.PreviewParameterData
import im.molly.monero.demo.ui.theme.AppIcons
import im.molly.monero.demo.ui.theme.AppTheme

@Composable
fun WalletRoute(
    walletId: Long,
    onTransactionClick: (String, Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.factory(walletId),
        key = WalletViewModel.key(walletId),
    )
) {
    val uiState: WalletUiState by viewModel.uiState.collectAsStateWithLifecycle()
    WalletScreen(
        uiState = uiState,
        onWalletConfigChange = { config -> viewModel.updateConfig(config) },
        onTransactionClick = onTransactionClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun WalletScreen(
    uiState: WalletUiState,
    onWalletConfigChange: (WalletConfig) -> Unit,
    onTransactionClick: (String, Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is WalletUiState.Loaded -> WalletScreenLoaded(
            uiState = uiState,
            onWalletConfigChange = onWalletConfigChange,
            onTransactionClick = onTransactionClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )

        WalletUiState.Error -> WalletScreenError(onBackClick = onBackClick)
        WalletUiState.Loading -> WalletScreenLoading(onBackClick = onBackClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletScreenLoaded(
    uiState: WalletUiState.Loaded,
    onWalletConfigChange: (WalletConfig) -> Unit,
    onTransactionClick: (String, Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        Toolbar(navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = "Back",
                )
            }
        }, actions = {
            WalletKebabMenu(
                onRenameClick = { showRenameDialog = true },
                onDeleteClick = { },
            )
        })
    }) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(style = MaterialTheme.typography.headlineMedium, text = buildAnnotatedString {
                append(MoneroCurrency.SYMBOL + " ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(
                        MoneroCurrency.Format(precision = 5).format(uiState.balance.confirmedAmount)
                    )
                }
            })
            Text(text = uiState.config.name, style = MaterialTheme.typography.headlineSmall)

            var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

            WalletHeaderTabs(
                titles = listOf("Balance", "Transactions"),
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index -> selectedTabIndex = index },
            )

            when (selectedTabIndex) {
                0 -> {
                    WalletBalanceView(balance = uiState.balance, blockchainTime = uiState.blockchainTime)
                }

                1 -> {
                    LazyColumn {
                        transactionCardItems(
                            items = uiState.transactions,
                            onTransactionClick = onTransactionClick,
                        )
                    }
                }
            }
        }

        if (showRenameDialog) {
            var name by remember { mutableStateOf(uiState.config.name) }
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
                        onWalletConfigChange(uiState.config.copy(name = name))
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
private fun WalletScreenError(
    onBackClick: () -> Unit,
) {
}

@Composable
private fun WalletScreenLoading(
    onBackClick: () -> Unit,
) {
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

@Composable
fun WalletHeaderTabs(
    titles: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Column {
        TabRow(selectedTabIndex = selectedTabIndex) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun WalletScreenPopulated(
    @PreviewParameter(WalletScreenPreviewParameterProvider::class) ledger: Ledger,
) {
    AppTheme {
        WalletScreen(
            uiState = WalletUiState.Loaded(
                config = WalletConfig(
                    id = 0,
                    publicAddress = ledger.primaryAddress.address,
                    filename = "",
                    name = "Personal",
                    remoteNodes = emptySet(),
                ),
                network = ledger.primaryAddress.network,
                balance = ledger.balance,
                blockchainTime = ledger.checkedAt,
                transactions = emptyList(),
            ),
            onWalletConfigChange = {},
            onTransactionClick = { _: String, _: Long -> },
            onBackClick = {},
        )
    }
}

private class WalletScreenPreviewParameterProvider : PreviewParameterProvider<Ledger> {
    override val values = sequenceOf(PreviewParameterData.ledger)
}
