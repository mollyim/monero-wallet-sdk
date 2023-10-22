package im.molly.monero.demo.ui

import TransactionCardExpanded
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import im.molly.monero.demo.data.model.WalletTransaction

fun LazyListScope.transactionCardItems(
    items: List<WalletTransaction>,
    onTransactionClick: (txId: String, walletId: Long) -> Unit,
    itemModifier: Modifier = Modifier,
) = items(
    items = items,
    key = { it.transaction.txId },
    itemContent = {
        TransactionCardExpanded(
            transaction = it.transaction,
            onClick = { onTransactionClick(it.transaction.txId, it.walletId) },
            modifier = itemModifier,
        )
    },
)
