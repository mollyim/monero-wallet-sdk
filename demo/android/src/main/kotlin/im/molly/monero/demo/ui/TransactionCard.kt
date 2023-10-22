import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import im.molly.monero.MoneroCurrency
import im.molly.monero.Transaction
import im.molly.monero.demo.ui.preview.PreviewParameterData
import im.molly.monero.demo.ui.theme.AppTheme
import im.molly.monero.demo.ui.theme.Blue40
import im.molly.monero.demo.ui.theme.Red40
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCardExpanded(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Card(
        onClick = onClick,
        modifier = modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = modifier.fillMaxWidth(),
            ) {
                val timestamp = transaction.timestamp?.let {
                    val localDateTime = it.atZone(ZoneId.systemDefault()).toLocalDateTime()
                    localDateTime.format(formatter)
                }
                Text(
                    text = timestamp ?: "",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = MoneroCurrency.format(transaction.amount, precision = 5),
                    color = if (transaction.amount < 0) Red40 else Blue40,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "#${transaction.blockHeight}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "fee ${MoneroCurrency.format(transaction.fee, precision = 8)}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
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

private class TransactionCardPreviewParameterProvider :
    PreviewParameterProvider<List<Transaction>> {
    override val values = sequenceOf(PreviewParameterData.transactions)
}
