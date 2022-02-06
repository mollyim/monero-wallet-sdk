package im.molly.monero.demo.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.demo.R
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.demo.ui.theme.AppTheme

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
    navigateToEditRemoteNode: (Long?) -> Unit,
) {
    val remoteNodes by viewModel.remoteNodes.collectAsStateWithLifecycle()

    SettingsScreen(
        remoteNodes = remoteNodes,
        modifier = modifier,
        onAddRemoteNode = { navigateToEditRemoteNode(RemoteNode.EMPTY.id) },
        onEditRemoteNode = { navigateToEditRemoteNode(it.id) },
        onDeleteRemoteNode = { viewModel.forgetRemoteNodeDetails(it) },
    )
}

@Composable
private fun SettingsScreen(
    remoteNodes: List<RemoteNode>,
    modifier: Modifier = Modifier,
    onAddRemoteNode: () -> Unit = {},
    onEditRemoteNode: (RemoteNode) -> Unit = {},
    onDeleteRemoteNode: (RemoteNode) -> Unit = {},
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(all = 24.dp)
    ) {
        Divider()
        SettingsSectionTitle(R.string.remote_nodes)
        TextButton(onClick = onAddRemoteNode) {
            Text(stringResource(R.string.add_remote_node))
        }
        RemoteNodeEditableList(
            remoteNodes = remoteNodes,
            onEditRemoteNode = onEditRemoteNode,
            onDeleteRemoteNode = onDeleteRemoteNode,
        )
//        Divider()
    }
}

@Composable
private fun SettingsSectionTitle(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            remoteNodes = emptyList(),
        )
    }
}
