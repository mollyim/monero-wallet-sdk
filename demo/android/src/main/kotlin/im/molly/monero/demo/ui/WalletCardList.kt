package im.molly.monero.demo.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier

fun LazyListScope.walletCardsItems(
    items: List<Long>,
    itemModifier: Modifier = Modifier,
) = items(
    items = items,
    key = { it },
    itemContent = {
        WalletCard(
            walletId = it,
            modifier = itemModifier,
        )
    },
)
