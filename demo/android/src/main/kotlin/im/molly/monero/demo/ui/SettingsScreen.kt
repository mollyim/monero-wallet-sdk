package im.molly.monero.demo.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.demo.R
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.demo.ui.theme.AppIcons
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
    ) {
        SettingsSection(
            header = {
                SettingsSectionTitle(R.string.remote_nodes)
                IconButton(onClick = onAddRemoteNode) {
                    Icon(
                        imageVector = AppIcons.AddRemoteWallet,
                        contentDescription = stringResource(R.string.add_remote_node),
                    )
                }
            }
        )
        RemoteNodeEditableList(
            remoteNodes = remoteNodes,
            onEditRemoteNode = onEditRemoteNode,
            onDeleteRemoteNode = onDeleteRemoteNode,
            modifier = Modifier.padding(start = 24.dp),
        )
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
        Divider(Modifier.padding(top = 24.dp))
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
private fun SettingsSectionTitle(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        val aNode = RemoteNode.EMPTY.copy(uri = Uri.parse("http://node.monero"))
        SettingsScreen(
            remoteNodes = listOf(aNode),
        )
    }
}
