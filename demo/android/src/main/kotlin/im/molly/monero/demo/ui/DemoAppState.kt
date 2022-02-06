package im.molly.monero.demo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import im.molly.monero.demo.ui.navigation.*

@Composable
fun rememberDemoAppState(
    navController: NavHostController = rememberNavController(),
): DemoAppState {
    return remember(navController) {
        DemoAppState(navController)
    }
}

@Stable
class DemoAppState(
    val navController: NavHostController,
) {
    val currentDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val topLevelDestinations = TopLevelDestination.values().asList()

    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        when (topLevelDestination) {
            TopLevelDestination.HOME -> navController.navigateToHome(topLevelNavOptions)
            TopLevelDestination.HISTORY -> navController.navigateToHistory(topLevelNavOptions)
            TopLevelDestination.SETTINGS -> navController.navigateToSettings(topLevelNavOptions)
        }
    }

    fun onBackClick() {
        navController.popBackStack()
    }
}
