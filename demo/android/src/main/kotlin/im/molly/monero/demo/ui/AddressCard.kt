package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import im.molly.monero.demo.data.model.WalletAddress
import im.molly.monero.demo.ui.component.CopyableText
import im.molly.monero.demo.ui.theme.Blue40
import im.molly.monero.demo.ui.theme.Red40

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
        with(walletAddress.address) {
            val used = walletAddress.used || isPrimaryAddress
            if (isPrimaryAddress) {
                Text(
                    text = "Account #$accountIndex Primary address",
                    style = MaterialTheme.typography.labelMedium,
                )
            } else {
                Text(
                    text = "Account #$accountIndex Subaddress #$subAddressIndex",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            CopyableText(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                modifier = if (used) Modifier.alpha(0.5f) else Modifier,
            )
            if (walletAddress.isLastForAccount) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onClick = onCreateSubAddressClick) {
                        Text(
                            text = "Add subaddress",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
