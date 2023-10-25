package im.molly.monero.demo.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.demo.ui.theme.AppIcons

@Composable
fun MultiSelectRemoteNodeList(
    remoteNodes: List<RemoteNode>,
    selectedIds: MutableMap<Long?, Boolean>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        if (remoteNodes.isNotEmpty()) {
            remoteNodes.forEach { remoteNode ->
                RemoteNodeItem(
                    remoteNode,
                    checked = selectedIds[remoteNode.id] ?: false,
                    showCheckbox = true,
                    onCheckedChange = { checked ->
                        selectedIds[remoteNode.id] = checked
                    },
                )
            }
        } else {
            Text(
                text = "Empty list. Start by adding new remote nodes in the settings.",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun RemoteNodeEditableList(
    remoteNodes: List<RemoteNode>,
    onEditRemoteNode: (RemoteNode) -> Unit,
    onDeleteRemoteNode: (RemoteNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        remoteNodes.forEach { remoteNode ->
            RemoteNodeItem(
                remoteNode,
                showMenu = true,
                onEditClick = onEditRemoteNode,
                onDeleteClick = onDeleteRemoteNode,
            )
        }
    }
}

@Composable
private fun RemoteNodeItem(
    remoteNode: RemoteNode,
    checked: Boolean = false,
    showCheckbox: Boolean = false,
    showMenu: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onEditClick: (RemoteNode) -> Unit = {},
    onDeleteClick: (RemoteNode) -> Unit = {},
) {
    ListItem(
        headlineContent = { Text(remoteNode.uri.toString()) },
        overlineContent = { Text(remoteNode.network.name.uppercase()) },
        trailingContent = {
            if (showCheckbox) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
            if (showMenu) {
                WalletKebabMenu(
                    onEditClick = { onEditClick(remoteNode) },
                    onDeleteClick = { onDeleteClick(remoteNode) },
                )
            }
        },
    )
}

@Composable
private fun WalletKebabMenu(
    onEditClick: () -> Unit,
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
            text = { Text("Edit") },
            onClick = {
                onEditClick()
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
private fun MultiSelectRemoteNodeListPreview() {
    val aNode = RemoteNode.EMPTY.copy(uri = Uri.parse("http://node.monero"))
    MultiSelectRemoteNodeList(
        remoteNodes = listOf(aNode),
        selectedIds = mutableMapOf(),
    )
}

@Preview
@Composable
private fun EmptyMultiSelectRemoteNodeListPreview() {
    val aNode = RemoteNode.EMPTY.copy(uri = Uri.parse("http://node.monero"))
    MultiSelectRemoteNodeList(
        remoteNodes = listOf(),
        selectedIds = mutableMapOf(),
    )
}
