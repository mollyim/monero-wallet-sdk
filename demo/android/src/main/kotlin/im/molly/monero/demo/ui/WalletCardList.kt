package im.molly.monero.demo.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier

fun LazyListScope.walletCardItems(
    items: List<Long>,
    onItemClick: (walletId: Long) -> Unit,
    itemModifier: Modifier = Modifier,
) = items(
    items = items,
    key = { it },
    itemContent = {
        WalletCard(
            walletId = it,
            onClick = { onItemClick(it) },
            modifier = itemModifier,
        )
    },
)
