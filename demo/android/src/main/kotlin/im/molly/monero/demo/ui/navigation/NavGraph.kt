package im.molly.monero.demo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun NavGraph(
    navController: NavHostController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = homeNavRoute,
) {
    NavHost(
        navController,
        startDestination,
        modifier,
    ) {
        homeScreen(
            navigateToWallet = { walletId ->
                navController.navigateToWallet(walletId)
            },
            navigateToAddWalletWizard = {
                navController.navigateToAddWalletWizardGraph()
            },
        )
        historyScreen(
            navigateToTransaction = { txId, walletId ->
                navController.navigateToTransaction(txId, walletId)
            },
        )
        settingsScreen(
            navigateToEditRemoteNode = { remoteNodeId ->
                navController.navigateToEditRemoteNode(remoteNodeId)
            },
        )
        walletScreen(
            navigateToTransaction = { txId, walletId ->
                navController.navigateToTransaction(txId, walletId)
            },
            onBackClick = onBackClick,
        )
        transactionScreen(
            onBackClick = onBackClick,
        )
        editRemoteNodeDialog(
            onBackClick = onBackClick,
        )
        addWalletWizardGraph(
            navController = navController,
            onBackClick = onBackClick,
        )
    }
}
