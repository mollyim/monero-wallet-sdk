package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.MoneroNetwork
import im.molly.monero.demo.R
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
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.add_wallet),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            )
        }
    ) { padding ->
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
                Text(stringResource(R.string.create_a_new_wallet))
            }
            OutlinedButton(
                onClick = {}, // TODO: onRestoreClick,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.i_already_have_a_wallet))
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
    val remoteNodes by viewModel.currentRemoteNodes.collectAsStateWithLifecycle()

    SecondStepScreen(
        showRestoreOptions = showRestoreOptions,
        modifier = modifier,
        onBackClick = onBackClick,
        onCreateClick = {
            viewModel.createWallet()
            onNavigateToHome()
        },
        walletName = viewModel.walletName,
        network = viewModel.network,
        onWalletNameChanged = { name -> viewModel.updateWalletName(name) },
        onNetworkChanged = { network -> viewModel.toggleSelectedNetwork(network) },
        remoteNodes = remoteNodes,
        selectedRemoteNodeIds = viewModel.selectedRemoteNodes,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondStepScreen(
    showRestoreOptions: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    walletName: String,
    network: MoneroNetwork,
    onWalletNameChanged: (String) -> Unit,
    onNetworkChanged: (MoneroNetwork) -> Unit,
    remoteNodes: List<RemoteNode>,
    selectedRemoteNodeIds: MutableMap<Long?, Boolean> = mutableMapOf(),
) {
    Scaffold(
        topBar = {
            val titleRes = if (showRestoreOptions) R.string.restore_wallet else R.string.new_wallet
            Toolbar(
                title = stringResource(titleRes),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = walletName,
                label = { Text(stringResource(R.string.wallet_name)) },
                onValueChange = onWalletNameChanged,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
            )
            SelectListBox(
                labelRes = R.string.network,
                options = MoneroNetwork.values().map { it.name },
                selectedOption = network.name,
                onOptionClick = {
                    onNetworkChanged(MoneroNetwork.valueOf(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            Text(
                text = stringResource(R.string.remote_node_selection),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(16.dp),
            )
            MultiSelectRemoteNodeList(
                remoteNodes = remoteNodes,
                selectedIds = selectedRemoteNodeIds,
                modifier = Modifier
                    .padding(start = 16.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .padding(16.dp),
                ) {
                    Text(stringResource(R.string.finish))
                }
            }
        }
    }
}

@Preview
@Composable
private fun CreateWalletScreenPreview() {
    AppTheme {
        SecondStepScreen(
            showRestoreOptions = false,
            onBackClick = {},
            onCreateClick = {},
            walletName = "Personal",
            network = DefaultMoneroNetwork,
            onWalletNameChanged = {},
            onNetworkChanged = {},
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
            onBackClick = {},
            onCreateClick = {},
            walletName = "Personal",
            network = DefaultMoneroNetwork,
            onWalletNameChanged = {},
            onNetworkChanged = {},
            remoteNodes = listOf(RemoteNode.EMPTY),
            selectedRemoteNodeIds = mutableMapOf(),
        )
    }
}
