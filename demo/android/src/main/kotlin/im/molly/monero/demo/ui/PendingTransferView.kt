package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import im.molly.monero.PendingTransfer
import im.molly.monero.toFormattedString

@Composable
fun PendingTransferView(
    spendingAccountIndex: Int,
    pendingTransfer: PendingTransfer,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Confirm transfer",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        with(pendingTransfer) {
            TextRow("Sending account", "#$spendingAccountIndex")
            TextRow("Amount", amount.toFormattedString())
            TextRow("Fee", fee.toFormattedString())
            TextRow("Transactions", txCount.toString())
        }
    }
}

@Composable
fun TextRow(label: String, text: String, modifier: Modifier = Modifier) {
    Text(
        text = "$label: $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(bottom = 8.dp),
    )
}
