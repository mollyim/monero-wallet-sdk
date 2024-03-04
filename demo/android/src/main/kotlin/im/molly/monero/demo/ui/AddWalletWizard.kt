package im.molly.monero.demo.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.MoneroNetwork
import im.molly.monero.demo.data.model.DefaultMoneroNetwork
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.demo.ui.component.SelectListBox
import im.molly.monero.demo.ui.component.Toolbar
import im.molly.monero.demo.ui.theme.AppIcons
import im.molly.monero.demo.ui.theme.AppTheme

@Composable
fun AddWalletFirstStepRoute(
    onBackClick: () -> Unit,
    onNavigateToCreateWallet: () -> Unit,
    onNavigateToRestoreWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FirstStepScreen(
        onBackClick = onBackClick,
        onCreateClick = onNavigateToCreateWallet,
        onRestoreClick = onNavigateToRestoreWallet,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstStepScreen(
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(topBar = {
        Toolbar(title = "Add wallet", navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = "Back",
                )
            }
        })
    }) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onCreateClick,
            ) {
                Text("Create a new wallet")
            }
            OutlinedButton(
                onClick = onRestoreClick,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("I already have a wallet")
            }
        }
    }
}

@Composable
fun AddWalletSecondStepRoute(
    showRestoreOptions: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHome: () -> Unit,
    viewModel: AddWalletViewModel = viewModel(),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remoteNodes by viewModel.currentRemoteNodes.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState.walletAdded) {
            onNavigateToHome()
        }
    }

    SecondStepScreen(
        showRestoreOptions = showRestoreOptions,
        modifier = modifier,
        onBackClick = onBackClick,
        onCreateClick = {
            if (showRestoreOptions) {
                viewModel.restoreWallet()
            } else {
                viewModel.createWallet()
            }
        },
        uiState = uiState,
        secretSpendKeyHexError = !viewModel.validateSecretSpendKeyHex(),
        creationDateError = !viewModel.validateCreationDate(),
        restoreHeightError = !viewModel.validateRestoreHeight(),
        onWalletNameChanged = { name -> viewModel.updateWalletName(name) },
        onNetworkChanged = { network -> viewModel.toggleSelectedNetwork(network) },
        onSecretSpendKeyHexChanged = { value -> viewModel.updateSecretSpendKeyHex(value) },
        onRecoverFromMnemonic = { words ->
            val success = viewModel.recoverFromMnemonic(words)
            if (!success) {
                Toast.makeText(context, "Invalid seed", Toast.LENGTH_LONG).show()
            }
        },
        onCreationDateChanged = { value -> viewModel.updateCreationDate(value) },
        onRestoreHeightChanged = { value -> viewModel.updateRestoreHeight(value) },
        remoteNodes = remoteNodes,
        selectedRemoteNodeIds = viewModel.selectedRemoteNodes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondStepScreen(
    showRestoreOptions: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    uiState: AddWalletUiState,
    secretSpendKeyHexError: Boolean = false,
    creationDateError: Boolean = false,
    restoreHeightError: Boolean = false,
    onWalletNameChanged: (String) -> Unit = {},
    onNetworkChanged: (MoneroNetwork) -> Unit = {},
    onSecretSpendKeyHexChanged: (String) -> Unit = {},
    onRecoverFromMnemonic: (String) -> Unit = {},
    onCreationDateChanged: (String) -> Unit = {},
    onRestoreHeightChanged: (String) -> Unit = {},
    remoteNodes: List<RemoteNode>,
    selectedRemoteNodeIds: MutableMap<Long?, Boolean> = mutableMapOf(),
) {
    var showOffLineConfirmationDialog by remember { mutableStateOf(false) }
    var showMnemonicDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        Toolbar(
            title = if (showRestoreOptions) "Restore wallet" else "New wallet",
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            })
    }) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = uiState.walletName,
                label = { Text("Wallet name") },
                onValueChange = onWalletNameChanged,
                singleLine = true,
                enabled = !uiState.isInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
            )
            SelectListBox(
                label = "Network",
                options = MoneroNetwork.entries.associateWith { it.name },
                selectedOption = uiState.network,
                onOptionClick = {
                    onNetworkChanged(it)
                },
                enabled = !uiState.isInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            Text(
                text = "Remote node selection",
                style = MaterialTheme.typography.titleMedium,
                modifier = (if (uiState.isInProgress) Modifier.alpha(0.3f) else Modifier)
                    .padding(16.dp),
            )
            MultiSelectRemoteNodeList(
                remoteNodes = remoteNodes,
                selectedIds = selectedRemoteNodeIds,
                enabled = !uiState.isInProgress,
                modifier = Modifier.padding(start = 16.dp),
            )
            if (showRestoreOptions) {
                Text(
                    text = "Deterministic wallet recovery",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = (if (uiState.isInProgress) Modifier.alpha(0.3f) else Modifier)
                        .padding(16.dp),
                )
                OutlinedTextField(
                    value = uiState.secretSpendKeyHex,
                    label = { Text("Secret spend key") },
                    onValueChange = onSecretSpendKeyHexChanged,
                    singleLine = true,
                    isError = secretSpendKeyHexError,
                    enabled = !uiState.isInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                )
                TextButton(
                    onClick = { showMnemonicDialog = true },
                    enabled = !uiState.isInProgress,
                    modifier = Modifier
                        .padding(start = 16.dp),
                ) {
                    Text("Recover from 25-word mnemonic")
                }
                Text(
                    text = "Synchronization",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                OutlinedTextField(
                    value = uiState.creationDate,
                    label = { Text("Wallet creation date") },
                    onValueChange = onCreationDateChanged,
                    singleLine = true,
                    isError = creationDateError,
                    enabled = uiState.restoreHeight.isEmpty() && !uiState.isInProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                )
                OutlinedTextField(
                    value = uiState.restoreHeight,
                    label = { Text("Restore height") },
                    onValueChange = onRestoreHeightChanged,
                    singleLine = true,
                    isError = restoreHeightError,
                    enabled = uiState.creationDate.isEmpty() && !uiState.isInProgress,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val validInput =
                    !showRestoreOptions || !(secretSpendKeyHexError || creationDateError || restoreHeightError)
                Button(
                    onClick = {
                        if (selectedRemoteNodeIds.filterValues { checked -> checked }.isEmpty()) {
                            showOffLineConfirmationDialog = true
                        } else {
                            onCreateClick()
                        }
                    },
                    enabled = validInput && !uiState.isInProgress,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(if (uiState.isInProgress) "Adding wallet..." else "Finish")
                }
            }
        }

        if (showMnemonicDialog) {
            var words by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showMnemonicDialog = false },
                title = { Text("Enter your recovery phrase") },
                text = {
                    OutlinedTextField(
                        value = words,
                        onValueChange = { words = it },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRecoverFromMnemonic(words)
                            showMnemonicDialog = false
                        },
                        enabled = words.isNotEmpty(),
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMnemonicDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showOffLineConfirmationDialog) {
            AlertDialog(onDismissRequest = { showOffLineConfirmationDialog = false }, title = {
                Text("No remote nodes selected")
            }, text = {
                Text("It seems there are no remote nodes added to your wallet settings. Are you sure you want to create an offline wallet?")
            }, confirmButton = {
                TextButton(onClick = {
                    showOffLineConfirmationDialog = false
                    onCreateClick()
                }) {
                    Text("Continue")
                }
            }, dismissButton = {
                TextButton(onClick = {
                    showOffLineConfirmationDialog = false
                }) {
                    Text("Cancel")
                }
            })
        }
    }
}

@Preview
@Composable
private fun CreateWalletScreenPreview() {
    AppTheme {
        SecondStepScreen(
            showRestoreOptions = false,
            uiState = AddWalletUiState(
                walletName = "Personal",
                network = DefaultMoneroNetwork,
                secretSpendKeyHex = "d2ca26e22489bd9871c910c58dee3ab08e66b9d566825a064c8c0af061cd8706",
            ),
            remoteNodes = listOf(RemoteNode.EMPTY),
            selectedRemoteNodeIds = mutableMapOf(),
        )
    }
}

@Preview
@Composable
private fun RestoreWalletScreenPreview() {
    AppTheme {
        SecondStepScreen(
            showRestoreOptions = true,
            uiState = AddWalletUiState(
                walletName = "Personal",
                network = DefaultMoneroNetwork,
                secretSpendKeyHex = "d2ca26e22489bd9871c910c58dee3ab08e66b9d566825a064c8c0af061cd8706",
            ),
            remoteNodes = listOf(RemoteNode.EMPTY),
            selectedRemoteNodeIds = mutableMapOf(),
        )
    }
}
