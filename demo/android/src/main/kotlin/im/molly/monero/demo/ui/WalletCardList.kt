package im.molly.monero.demo.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier

fun LazyListScope.walletCardsItems(
    items: List<Long>,
    onItemClick: (Long) -> Unit,
    itemModifier: Modifier = Modifier,
) = items(
    items = items,
    key = { it },
    itemContent = {
        WalletCard(
            walletId = it,
            onClick = onItemClick,
            modifier = itemModifier,
        )
    },
)
