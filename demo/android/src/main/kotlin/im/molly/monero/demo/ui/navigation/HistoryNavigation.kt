package im.molly.monero.demo.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import im.molly.monero.demo.ui.HistoryRoute

const val historyNavRoute = "history"

fun NavController.navigateToHistory(navOptions: NavOptions? = null) {
    navigate(historyNavRoute, navOptions)
}

fun NavGraphBuilder.historyScreen() {
    composable(route = historyNavRoute) {
        HistoryRoute()
    }
}
