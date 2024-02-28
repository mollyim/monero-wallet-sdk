package im.molly.monero.demo.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import im.molly.monero.demo.data.model.WalletAddress

fun LazyListScope.addressCardItems(
    items: List<WalletAddress>,
    onCreateSubAddressClick: (accountIndex: Int) -> Unit,
    itemModifier: Modifier = Modifier,
) = items(
    items = items,
    key = { it.address },
    itemContent = {
        AddressCardExpanded(
            walletAddress = it,
            onClick = { },
            onCreateSubAddressClick = {
                onCreateSubAddressClick(it.address.accountIndex)
            },
            modifier = itemModifier,
        )
    },
)
