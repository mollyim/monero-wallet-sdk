package im.molly.monero.demo.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import im.molly.monero.demo.ui.component.Toolbar

@Composable
fun HistoryRoute(
    navigateToTransaction: (String, Long) -> Unit,
) {
    HistoryScreen(
        onTransactionClick = navigateToTransaction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    onTransactionClick: (String, Long) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Toolbar(
                title = "Transaction history",
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        // TODO
    }
}
