package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import im.molly.monero.sdk.MoneroCurrency

@Composable
fun EditableRecipientList(
    recipients: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRecipientChange: (List<Pair<String, String>>) -> Unit = {},
) {
    val updatedList = recipients.toMutableList()

    if (updatedList.isEmpty()) {
        updatedList.add("" to "")
        onRecipientChange(updatedList)
    }

    Column(modifier = modifier) {
        updatedList.forEachIndexed { index, (address, amount) ->
            PaymentDetailItem(
                itemIndex = index,
                amount = amount,
                address = address,
                enabled = enabled,
                onAmountChange = {
                    updatedList[index] = address to it
                    onRecipientChange(updatedList)
                },
                onAddressChange = {
                    updatedList[index] = it to amount
                    onRecipientChange(updatedList)
                },
                onDeleteItemClick = {
                    if (updatedList.size > 1) {
                        updatedList.removeAt(index)
                    } else {
                        updatedList[0] = "" to ""
                    }
                    onRecipientChange(updatedList)
                }
            )
        }
        TextButton(
            onClick = { onRecipientChange(updatedList + ("" to "")) },
            enabled = enabled,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(text = "Add recipient")
        }
    }
}

@Composable
fun PaymentDetailItem(
    itemIndex: Int,
    address: String,
    amount: String,
    enabled: Boolean = true,
    onAmountChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onDeleteItemClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        OutlinedTextField(
            label = { Text("Recipient") },
            singleLine = true,
            value = address,
            isError = address.isBlank(),
            enabled = enabled,
            onValueChange = onAddressChange,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            label = { Text("Amount") },
            placeholder = { Text("0.00") },
            singleLine = true,
            value = amount,
            suffix = { Text(MoneroCurrency.SYMBOL) },
            isError = amount.isBlank(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            onValueChange = onAmountChange,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        IconButton(
            onClick = onDeleteItemClick,
            enabled = enabled,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@Preview
@Composable
private fun EditableRecipientListPreview() {
    EditableRecipientList(
        recipients = listOf(
            "888tNkZrPN6JsEgekjMnABU4TBzc2Dt29EPAvkRxbANsAnjyPbb3iQ1YBRk1UXcdRsiKc9dhwMVgN5S9cQUiyoogDavup3H" to "0.01"
        ),
    )
}
