package im.molly.monero.demo.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import im.molly.monero.demo.ui.HomeRoute

const val homeNavRoute = "home"

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    navigate(homeNavRoute, navOptions)
}

fun NavGraphBuilder.homeScreen(
    navigateToAddWalletWizard: () -> Unit,
) {
    composable(route = homeNavRoute) {
        HomeRoute(
            navigateToAddWalletWizard = navigateToAddWalletWizard,
        )
    }
}
