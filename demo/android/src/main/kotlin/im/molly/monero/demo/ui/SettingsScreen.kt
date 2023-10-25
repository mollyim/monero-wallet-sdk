package im.molly.monero.demo.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.demo.data.model.UserSettings
import im.molly.monero.demo.data.model.toSocketAddress
import im.molly.monero.demo.ui.theme.AppIcons
import im.molly.monero.demo.ui.theme.AppTheme

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    navigateToEditRemoteNode: (Long?) -> Unit,
) {
    val settingsUiState by viewModel.settingsUiState.collectAsStateWithLifecycle()

    SettingsScreen(
        settingsUiState = settingsUiState,
        modifier = modifier,
        onAddRemoteNode = { navigateToEditRemoteNode(RemoteNode.EMPTY.id) },
        onEditRemoteNode = { navigateToEditRemoteNode(it.id) },
        onDeleteRemoteNode = { viewModel.forgetRemoteNodeDetails(it) },
        onChangeSocksProxy = { viewModel.setSocksProxyAddress(it) },
        onValidateSocksProxy = { viewModel.isSocksProxyAddressCorrect(it) },
    )
}

@Composable
private fun SettingsScreen(
    settingsUiState: SettingsUiState,
    modifier: Modifier = Modifier,
    onAddRemoteNode: () -> Unit = {},
    onEditRemoteNode: (RemoteNode) -> Unit = {},
    onDeleteRemoteNode: (RemoteNode) -> Unit = {},
    onChangeSocksProxy: (String) -> Unit = {},
    onValidateSocksProxy: (String) -> Boolean = { true },
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        when (settingsUiState) {
            SettingsUiState.Loading -> {
                // TODO: Add loading wheel
            }
            is SettingsUiState.Success -> {
                SettingsSection(
                    header = {
                        SettingsSectionTitle("Remote nodes")
                        IconButton(onClick = onAddRemoteNode) {
                            Icon(
                                imageVector = AppIcons.AddRemoteWallet,
                                contentDescription = "Add remote node",
                            )
                        }
                    }
                )
                RemoteNodeEditableList(
                    remoteNodes = settingsUiState.remoteNodes,
                    onEditRemoteNode = onEditRemoteNode,
                    onDeleteRemoteNode = onDeleteRemoteNode,
                    modifier = Modifier.padding(start = 24.dp),
                )
                SettingsSection(
                    header = {
                        SettingsSectionTitle("Network")
                    }
                ) {
                    EditTextSettingsItem(
                        title = "SOCKS proxy server",
                        summary = "Connect via proxy.",
                        value = settingsUiState.socksProxyAddress,
                        onValueChange = onChangeSocksProxy,
                        inputHeading = "Provide proxy address or leave it blank for no proxy.",
                        inputLabel = "host:port",
                        inputChecker = onValidateSocksProxy,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    header: @Composable() (RowScope.() -> Unit),
    content: @Composable() (RowScope.() -> Unit) = {},
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
    ) {
        Divider(Modifier.padding(bottom = 24.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth(),
            content = header,
        )
        Row(
            content = content,
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun EditTextSettingsItem(
    title: String,
    summary: String,
    value: String,
    onValueChange: (String) -> Unit,
    inputLabel: String,
    inputHeading: String = title,
    inputChecker: (String) -> Boolean = { true },
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Text(value) },
        modifier = Modifier
            .clickable(onClick = { showDialog = true }),
    )

    if (showDialog) {
        var input by remember { mutableStateOf(value) }
        val valid = inputChecker(input)

        AlertDialog(
            text = {
                Column {
                    Text(
                        text = inputHeading,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                    OutlinedTextField(
                        label = { Text(inputLabel) },
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        isError = !valid,
                    )
                }
            },
            onDismissRequest = { showDialog = false },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        onValueChange(input)
                        showDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPopulatedPreview() {
    AppTheme {
        val aNode = RemoteNode.EMPTY.copy(uri = Uri.parse("http://node.monero"))
        SettingsScreen(
            SettingsUiState.Success(
                socksProxyAddress = "localhost:9050",
                remoteNodes = listOf(aNode),
            )
        )
    }
}
