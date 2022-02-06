package im.molly.monero.demo.ui.navigation

import androidx.navigation.*
import androidx.navigation.compose.composable
import im.molly.monero.demo.ui.AddWalletFirstStepRoute
import im.molly.monero.demo.ui.AddWalletSecondStepRoute

const val addWalletWizardNavRoute = "home/add_wallet_wizard"

private const val startNavRoute = "$addWalletWizardNavRoute/start"
private const val createNavRoute = "$addWalletWizardNavRoute/create"
private const val restoreNavRoute = "$addWalletWizardNavRoute/restore"

fun NavController.navigateToAddWalletWizardGraph(navOptions: NavOptions? = null) {
    navigate(addWalletWizardNavRoute, navOptions)
}

fun NavController.navigateToAddWalletSecondStep(restoreWallet: Boolean) {
    when (restoreWallet) {
        true -> navigate(restoreNavRoute)
        false -> navigate(createNavRoute)
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
