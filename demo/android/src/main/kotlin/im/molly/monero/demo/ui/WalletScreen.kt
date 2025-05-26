package im.molly.monero.demo.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.demo.data.model.WalletConfig
import im.molly.monero.demo.ui.component.SelectListBox
import im.molly.monero.demo.ui.component.Toolbar
import im.molly.monero.demo.ui.preview.PreviewParameterData
import im.molly.monero.demo.ui.theme.AppIcons
import im.molly.monero.demo.ui.theme.AppTheme
import im.molly.monero.sdk.FeePriority
import im.molly.monero.sdk.Ledger
import im.molly.monero.sdk.MoneroCurrency
import im.molly.monero.sdk.PendingTransfer
import im.molly.monero.sdk.toFormattedString
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Composable
fun WalletRoute(
    walletId: Long,
    onTransactionClick: (String, Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    walletViewModel: WalletViewModel = viewModel(
        factory = WalletViewModel.factory(walletId),
        key = WalletViewModel.key(walletId),
    ),
    sendTabViewModel: SendTabViewModel = viewModel(
        factory = SendTabViewModel.factory(walletId)
    ),
) {
    val walletUiState by walletViewModel.uiState.collectAsStateWithLifecycle()
    val sendTabUiState by sendTabViewModel.uiState.collectAsStateWithLifecycle()

    WalletScreen(
        walletUiState = walletUiState,
        sendTabUiState = sendTabUiState,
        onWalletConfigChange = { config ->
            walletViewModel.updateConfig(config)
        },
        onTransactionClick = onTransactionClick,
        onCreateAccountClick = { walletViewModel.createAccount() },
        onCreateSubAddressClick = { accountIndex ->
            walletViewModel.createSubAddress(accountIndex)
        },
        onTransferAccountSelect = { sendTabViewModel.updateAccount(it) },
        onTransferPrioritySelect = { sendTabViewModel.updatePriority(it) },
        onTransferRecipientChange = { sendTabViewModel.updateRecipients(it) },
        onTransferSendClick = { sendTabViewModel.createPayment() },
        onTransferConfirmClick = { sendTabViewModel.confirmPendingTransfer() },
        onTransferCancelClick = { sendTabViewModel.cancelPendingTransfer() },
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun WalletScreen(
    walletUiState: WalletUiState,
    sendTabUiState: SendTabUiState,
    modifier: Modifier = Modifier,
    onWalletConfigChange: (WalletConfig) -> Unit = {},
    onTransactionClick: (String, Long) -> Unit = { _, _ -> },
    onCreateAccountClick: () -> Unit = {},
    onCreateSubAddressClick: (Int) -> Unit = {},
    onTransferAccountSelect: (Int) -> Unit = {},
    onTransferPrioritySelect: (FeePriority) -> Unit = {},
    onTransferRecipientChange: (List<Pair<String, String>>) -> Unit = {},
    onTransferSendClick: () -> Unit = {},
    onTransferConfirmClick: () -> Unit = {},
    onTransferCancelClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    when (walletUiState) {
        is WalletUiState.Loaded -> WalletScreenLoaded(
            walletUiState = walletUiState,
            sendTabUiState = sendTabUiState,
            onWalletConfigChange = onWalletConfigChange,
            onTransactionClick = onTransactionClick,
            onCreateAccountClick = onCreateAccountClick,
            onCreateSubAddressClick = onCreateSubAddressClick,
            onTransferAccountSelect = onTransferAccountSelect,
            onTransferPrioritySelect = onTransferPrioritySelect,
            onTransferRecipientChange = onTransferRecipientChange,
            onTransferSendClick = onTransferSendClick,
            onTransferConfirmClick = onTransferConfirmClick,
            onTransferCancelClick = onTransferCancelClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )

        WalletUiState.Error -> WalletScreenError(onBackClick = onBackClick)
        WalletUiState.Loading -> WalletScreenLoading(onBackClick = onBackClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletToolbar(
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Toolbar(navigationIcon = {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = AppIcons.ArrowBack,
                contentDescription = "Back",
            )
        }
    }, actions = actions)
}

@Composable
private fun WalletScreenLoaded(
    walletUiState: WalletUiState.Loaded,
    sendTabUiState: SendTabUiState,
    onWalletConfigChange: (WalletConfig) -> Unit,
    onTransactionClick: (String, Long) -> Unit,
    onCreateAccountClick: () -> Unit,
    onCreateSubAddressClick: (Int) -> Unit,
    onTransferAccountSelect: (Int) -> Unit,
    onTransferPrioritySelect: (FeePriority) -> Unit,
    onTransferRecipientChange: (List<Pair<String, String>>) -> Unit,
    onTransferSendClick: () -> Unit,
    onTransferConfirmClick: () -> Unit,
    onTransferCancelClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            WalletToolbar(onBackClick = onBackClick, actions = {
                WalletKebabMenu(
                    onRenameClick = { showRenameDialog = true },
                    onDeleteClick = { },
                )
            })
        },
    ) { padding ->
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
                        MoneroCurrency.Format(precision = 5)
                            .format(walletUiState.totalBalance.confirmedAmount)
                    )
                }
            })
            Text(text = walletUiState.config.name, style = MaterialTheme.typography.headlineSmall)

            WalletHeaderTabs(
                titles = listOf("Balance", "Send", "Receive", "History"),
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index -> selectedTabIndex = index },
            )

            when (selectedTabIndex) {
                0 -> {
                    WalletBalanceView(
                        balance = walletUiState.totalBalance,
                        blockchainTime = walletUiState.blockchainTime
                    )
                }

                1 -> {
                    WalletSendTab(
                        walletUiState, sendTabUiState,
                        onAccountSelect = onTransferAccountSelect,
                        onPrioritySelect = onTransferPrioritySelect,
                        onRecipientChange = onTransferRecipientChange,
                        onSendClick = onTransferSendClick,
                    )
                }

                2 -> {
                    val scrollState = rememberLazyListState()

                    LazyColumn(
                        state = scrollState,
                    ) {
                        addressCardItems(
                            items = walletUiState.addresses,
                            onCreateSubAddressClick = onCreateSubAddressClick,
                        )
                        item {
                            TextButton(
                                onClick = onCreateAccountClick,
                                modifier = modifier.padding(start = 16.dp, bottom = 8.dp),
                            ) {
                                Text(
                                    text = "Create new account",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                3 -> {
                    val scrollState = rememberLazyListState()

                    var lastTxId by remember { mutableStateOf("") }
                    lastTxId = walletUiState.transactions.firstOrNull()?.transaction?.txId ?: ""

                    LaunchedEffect(lastTxId) {
                        if (walletUiState.transactions.isNotEmpty()) {
                            scrollState.scrollToItem(0)
                        }
                    }

                    LazyColumn(
                        state = scrollState,
                    ) {
                        transactionCardItems(
                            items = walletUiState.transactions,
                            onTransactionClick = onTransactionClick,
                        )
                    }
                }
            }
        }

        if (showRenameDialog) {
            var name by remember { mutableStateOf(walletUiState.config.name) }
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
                        onWalletConfigChange(walletUiState.config.copy(name = name))
                        showRenameDialog = false
                    }) {
                        Text("Rename")
                    }
                },
            )
        }

        if (sendTabUiState.status is TransferStatus.ReadyForApproval) {
            SendConfirmationDialog(
                spendingAccountIndex = sendTabUiState.accountIndex,
                pendingTransfer = sendTabUiState.status.pendingTransfer,
                onConfirmRequest = {
                    onTransferConfirmClick()
                },
                onDismissRequest = {
                    Toast.makeText(context, "Transfer canceled", Toast.LENGTH_LONG).show()
                    onTransferCancelClick()
                },
            )
        }

        LaunchedEffect(sendTabUiState.status) {
            if (sendTabUiState.status == TransferStatus.Sent) {
                Toast.makeText(context, "Transfer submitted", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
private fun WalletSendTab(
    walletUiState: WalletUiState.Loaded,
    sendTabUiState: SendTabUiState,
    onAccountSelect: (Int) -> Unit,
    onPrioritySelect: (FeePriority) -> Unit,
    onRecipientChange: (List<Pair<String, String>>) -> Unit,
    onSendClick: () -> Unit,
) {
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(now) {
        delay(2.seconds)
        now = Instant.now()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SelectListBox(
            label = "Send from",
            options = walletUiState.accountBalance.mapValues { (index, balance) ->
                val currentTime = walletUiState.blockchainTime.withTimestamp(now)
                val funds = balance.unlockedAmountAt(currentTime)
                val fundsFormatted = funds.toFormattedString(appendSymbol = true)
                "Account #$index : $fundsFormatted"
            },
            selectedOption = sendTabUiState.accountIndex,
            onOptionClick = { option -> onAccountSelect(option) },
            enabled = !sendTabUiState.isInProgress,
        )
        EditableRecipientList(
            recipients = sendTabUiState.recipients,
            onRecipientChange = onRecipientChange,
            enabled = !sendTabUiState.isInProgress,
        )
        SelectListBox(
            label = "Transaction priority",
            options = FeePriority.entries.associateWith { it.name },
            selectedOption = sendTabUiState.feePriority,
            onOptionClick = { option -> onPrioritySelect(option) },
            enabled = !sendTabUiState.isInProgress,
        )

        if (sendTabUiState.status is TransferStatus.Error) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Error Icon",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = sendTabUiState.status.errorMessage
                        ?: "Unspecific error",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        FilledTonalButton(
            modifier = Modifier.padding(vertical = 24.dp),
            onClick = onSendClick,
            enabled = !sendTabUiState.isInProgress,
        ) {
            val text = when (sendTabUiState.status) {
                TransferStatus.Preparing -> "Processing..."
                is TransferStatus.ReadyForApproval -> "Processing..."
                TransferStatus.Sending -> "Sending..."
                else -> "Send"
            }
            Text(text = text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendConfirmationDialog(
    spendingAccountIndex: Int,
    pendingTransfer: PendingTransfer,
    onConfirmRequest: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            PendingTransferView(
                spendingAccountIndex = spendingAccountIndex,
                pendingTransfer = pendingTransfer,
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }
                FilledTonalButton(
                    onClick = onConfirmRequest,
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
private fun WalletScreenError(
    onBackClick: () -> Unit,
) {
    Scaffold(topBar = { WalletToolbar(onBackClick = onBackClick) }) {}
}

@Composable
private fun WalletScreenLoading(
    onBackClick: () -> Unit,
) {
    Scaffold(topBar = { WalletToolbar(onBackClick = onBackClick) }) {}
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
            walletUiState = WalletUiState.Loaded(
                config = WalletConfig(
                    id = 0,
                    publicAddress = ledger.publicAddress.address,
                    filename = "",
                    name = "Personal",
                    remoteNodes = emptySet(),
                ),
                network = ledger.publicAddress.network,
                totalBalance = ledger.getBalance(),
                accountBalance = emptyMap(),
                blockchainTime = ledger.checkedAt,
                addresses = emptyList(),
                transactions = emptyList(),
            ),
            sendTabUiState = SendTabUiState(
                accountIndex = 0,
                recipients = emptyList(),
                feePriority = FeePriority.Medium,
            )
        )
    }
}

private class WalletScreenPreviewParameterProvider : PreviewParameterProvider<Ledger> {
    override val values = sequenceOf(PreviewParameterData.ledger)
}
