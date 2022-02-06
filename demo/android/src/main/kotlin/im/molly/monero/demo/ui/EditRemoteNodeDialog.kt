package im.molly.monero.demo.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import im.molly.monero.MoneroNetwork
import im.molly.monero.demo.R
import im.molly.monero.demo.data.model.RemoteNode
import im.molly.monero.demo.ui.component.SelectListBox
import im.molly.monero.demo.ui.theme.AppTheme
import kotlinx.coroutines.flow.first

@Composable
fun EditRemoteNodeRoute(
    remoteNodeId: Long?,
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    var remoteNode by remember { mutableStateOf(RemoteNode.EMPTY) }

    remoteNodeId?.let {
        LaunchedEffect(it) {
            remoteNode = viewModel.remoteNode(it).first()
        }
    }

    EditRemoteNodeDialog(
        remoteNode = remoteNode,
        showError = !viewModel.isRemoteNodeCorrect(remoteNode),
        onRemoteNodeChange = { remoteNode = it },
        onSaveRequest = {
            viewModel.saveRemoteNodeDetails(remoteNode)
            onBackClick()
        },
        onDismissRequest = onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRemoteNodeDialog(
    remoteNode: RemoteNode,
    showError: Boolean,
    onRemoteNodeChange: (RemoteNode) -> Unit,
    onSaveRequest: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.enter_your_monero_node_information_below),
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                SelectListBox(
                    labelRes = R.string.network,
                    options = MoneroNetwork.values().map { it.name },
                    selectedOption = remoteNode.network.name,
                    onOptionClick = {
                        onRemoteNodeChange(remoteNode.copy(network = MoneroNetwork.valueOf(it)))
                    }
                )
                Column {
                    OutlinedTextField(
                        value = remoteNode.uri.toString(),
                        onValueChange = { input ->
                            onRemoteNodeChange(remoteNode.copy(uri = Uri.parse(input)))
                        },
                        label = {
                            Text(stringResource(R.string.url))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        isError = showError,
                    )
                    Text(
                        text = stringResource(R.string.protocol_is_required_http_or_https),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
                    )
                }
                OutlinedTextField(
                    value = remoteNode.username,
                    onValueChange = { input ->
                        onRemoteNodeChange(remoteNode.copy(username = input))
                    },
                    label = {
                        Text(stringResource(R.string.username))
                    },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = remoteNode.password,
                    onValueChange = { input ->
                        onRemoteNodeChange(remoteNode.copy(password = input))
                    },
                    label = {
                        Text(stringResource(R.string.password))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSaveRequest,
                enabled = !showError,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun EditRemoteNodeDialogPreview() {
    AppTheme {
        EditRemoteNodeDialog(
            remoteNode = RemoteNode.EMPTY,
            showError = false,
            onRemoteNodeChange = {},
            onSaveRequest = {},
            onDismissRequest = {},
        )
    }
}
