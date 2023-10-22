import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import im.molly.monero.MoneroCurrency
import im.molly.monero.Transaction
import im.molly.monero.demo.ui.preview.PreviewParameterData
import im.molly.monero.demo.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCardExpanded(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            TransactionDetail("State", transaction.state.toString())
            Spacer(modifier = Modifier.height(12.dp))
            TransactionDetail("Sent", transaction.sent.toString())
            Spacer(modifier = Modifier.height(12.dp))
            TransactionDetail("Received", transaction.received.toString())
            Spacer(modifier = Modifier.height(12.dp))
            TransactionDetail("Payments", transaction.payments.toString())
            Spacer(modifier = Modifier.height(12.dp))
            TransactionDetail("Time lock", transaction.timeLock.toString())
            Spacer(modifier = Modifier.height(12.dp))
            TransactionDetail("Change", MoneroCurrency.format(transaction.change))
            Spacer(modifier = Modifier.height(12.dp))
            TransactionDetail("Fee", MoneroCurrency.format(transaction.fee))
            Spacer(modifier = Modifier.height(12.dp))
            TransactionDetail("Transaction ID", transaction.txId)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TransactionDetail(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = modifier)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}

@Preview
@Composable
private fun TransactionCardExpandedPreview(
    @PreviewParameter(
        TransactionCardPreviewParameterProvider::class,
        limit = 1,
    ) transactions: List<Transaction>,
) {
    AppTheme {
        Surface {
            TransactionCardExpanded(
                transaction = transactions.first(),
                onClick = {},
            )
        }
    }
}

private class TransactionCardPreviewParameterProvider : PreviewParameterProvider<List<Transaction>> {
    override val values = sequenceOf(PreviewParameterData.transactions)
}
