package im.molly.monero.demo.ui.navigation

import androidx.navigation.*
import androidx.navigation.compose.composable
import im.molly.monero.demo.ui.AddWalletFirstStepRoute
import im.molly.monero.demo.ui.AddWalletSecondStepRoute
import im.molly.monero.demo.ui.WalletRoute

const val walletNavRoute = "wallet"
const val addWalletWizardNavRoute = "add_wallet_wizard"

private const val walletIdArg = "id"

private const val startNavRoute = "$addWalletWizardNavRoute/start"
private const val createNavRoute = "$addWalletWizardNavRoute/create"
private const val restoreNavRoute = "$addWalletWizardNavRoute/restore"

fun NavController.navigateToWallet(walletId: Long) {
    val route = "$walletNavRoute/$walletId"
    navigate(route)
}

fun NavController.navigateToAddWalletWizardGraph(navOptions: NavOptions? = null) {
    navigate(addWalletWizardNavRoute, navOptions)
}

fun NavController.navigateToAddWalletSecondStep(restoreWallet: Boolean) {
    when (restoreWallet) {
        true -> navigate(restoreNavRoute)
        false -> navigate(createNavRoute)
    }
}

fun NavGraphBuilder.walletScreen(
    navigateToTransaction: (String, Long) -> Unit,
    onBackClick: () -> Unit,
) {
    composable(
        route = "$walletNavRoute/{$walletIdArg}",
        arguments = listOf(
            navArgument(walletIdArg) { type = NavType.LongType }
        )
    ) {
        val arguments = requireNotNull(it.arguments)
        val walletId = arguments.getLong(walletIdArg)
        WalletRoute(
            walletId = walletId,
            onTransactionClick = navigateToTransaction,
            onBackClick = onBackClick,
        )
    }
}

fun NavGraphBuilder.addWalletWizardGraph(
    navController: NavHostController,
    onBackClick: () -> Unit,
) {
    navigation(
        route = addWalletWizardNavRoute,
        startDestination = startNavRoute,
    ) {
        composable(route = startNavRoute) {
            AddWalletFirstStepRoute(
                onBackClick = onBackClick,
                onNavigateToCreateWallet = {
                    navController.navigateToAddWalletSecondStep(restoreWallet = false)
                },
                onNavigateToRestoreWallet = {
                    navController.navigateToAddWalletSecondStep(restoreWallet = true)
                },
            )
        }
        composable(route = createNavRoute) {
            AddWalletSecondStepRoute(
                showRestoreOptions = false,
                onBackClick = onBackClick,
                onNavigateToHome = { navController.navigateToHome() },
            )
        }
        composable(route = restoreNavRoute) {
            AddWalletSecondStepRoute(
                showRestoreOptions = true,
                onBackClick = onBackClick,
                onNavigateToHome = { navController.navigateToHome() },
            )
        }
    }
}
