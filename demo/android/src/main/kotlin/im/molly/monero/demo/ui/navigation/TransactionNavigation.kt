package im.molly.monero.demo.ui.navigation

import androidx.navigation.*
import androidx.navigation.compose.composable
import im.molly.monero.demo.ui.TransactionRoute

const val transactionNavRoute = "tx"

private const val txIdArg = "txId"
private const val walletIdArg = "walletId"

fun NavController.navigateToTransaction(txId: String, walletId: Long) {
    val route = "$transactionNavRoute/$txId/$walletId"
    navigate(route)
}

fun NavGraphBuilder.transactionScreen(
    onBackClick: () -> Unit,
) {
    composable(
        route = "$transactionNavRoute/{$txIdArg}/{$walletIdArg}",
        arguments = listOf(
            navArgument(txIdArg) { type = NavType.StringType },
            navArgument(walletIdArg) { type = NavType.LongType },
        )
    ) {
        val arguments = requireNotNull(it.arguments)
        val txId = arguments.getString(txIdArg)
        val walletId = arguments.getLong(walletIdArg)
        TransactionRoute(
            txId = txId!!,
            walletId = walletId,
            onBackClick = onBackClick,
        )
    }
}
