package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import im.molly.monero.demo.data.model.WalletAddress
import im.molly.monero.demo.ui.component.CopyableText
import im.molly.monero.sdk.calculateBalance
import im.molly.monero.sdk.toFormattedString

@Composable
fun AddressCardExpanded(
    walletAddress: WalletAddress,
    onClick: () -> Unit,
    onCreateSubAddressClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val enotesCount = walletAddress.enotes.count()
        val unspentCount = walletAddress.enotes.count { !it.value.spent }
        val totalAmount = walletAddress.enotes.calculateBalance().totalAmount

        with(walletAddress.address) {
            val addressText = if (isPrimaryAddress) {
                "Account #$accountIndex Primary address"
            } else {
                "Account #$accountIndex Subaddress #$subAddressIndex"
            }

            val markedUsed = walletAddress.used || isPrimaryAddress

            Text(
                text = addressText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Total balance: ${totalAmount.toFormattedString(appendSymbol = true)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Total owned enotes: $enotesCount",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Unspent enotes: $unspentCount",
                style = MaterialTheme.typography.bodySmall,
            )
            CopyableText(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                modifier = if (markedUsed) Modifier.alpha(0.5f) else Modifier,
            )
            if (walletAddress.isLastForAccount) {
                TextButton(onClick = onCreateSubAddressClick) {
                    Text(
                        text = "Add subaddress",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

        }
    }
}
