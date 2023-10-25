package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import im.molly.monero.MoneroAmount
import im.molly.monero.Balance
import im.molly.monero.BlockchainTime
import im.molly.monero.MoneroCurrency
import im.molly.monero.TimeLocked
import im.molly.monero.demo.ui.theme.AppTheme
import im.molly.monero.xmr
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

@Composable
fun WalletBalanceView(
    balance: Balance,
    blockchainTime: BlockchainTime,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(now) {
        delay(1.seconds)
        now = Instant.now()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            style = MaterialTheme.typography.bodyLarge,
            text = "Balance at ${blockchainTime}",
        )

        Spacer(modifier = Modifier.height(12.dp))

        BalanceRow("Confirmed", balance.confirmedAmount)
        BalanceRow("Pending", balance.pendingAmount)
        Divider()
        BalanceRow("Total", balance.totalAmount)

        BalanceRow("Unlocked", balance.unlockedAmountAt(blockchainTime.height, now))
        balance.lockedAmountsAt(blockchainTime.height, now).forEach { (timeSpan, amount) ->
            LockedBalanceRow("Locked", amount, timeSpan.blocks, timeSpan.timeRemaining)
        }
    }
}

@Composable
fun BalanceRow(
    label: String,
    amount: MoneroAmount,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = MoneroCurrency.format(amount), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LockedBalanceRow(
    label: String,
    amount: MoneroAmount,
    blockCount: Int,
    timeRemaining: Duration,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val durationText =
            "${timeRemaining.toKotlinDuration()} ($blockCount blocks)"
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = durationText, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = MoneroCurrency.format(amount), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
fun WalletBalanceDetailsPreview() {
    AppTheme {
        WalletBalanceView(
            balance = Balance(
                pendingAmount = 5.xmr,
                timeLockedAmounts = listOf(
                    TimeLocked(10.xmr, BlockchainTime.Genesis),
                    TimeLocked(BigDecimal("0.000000000001").xmr, BlockchainTime.Block(10)),
                    TimeLocked(30.xmr, BlockchainTime.Block(500)),
                ),
            ),
            blockchainTime = BlockchainTime.Genesis,
        )
    }
}
