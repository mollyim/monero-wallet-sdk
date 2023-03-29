package im.molly.monero.demo.ui.navigation

import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import im.molly.monero.demo.ui.EditRemoteNodeRoute
import im.molly.monero.demo.ui.SettingsRoute

const val settingsNavRoute = "settings"
const val settingsRemoteNodeNavRoute = "$settingsNavRoute/remote_node"

private const val queryId = "id"

fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(settingsNavRoute, navOptions)
}

fun NavController.navigateToEditRemoteNode(remoteNodeId: Long?) {
    val route = settingsRemoteNodeNavRoute +
            if (remoteNodeId != null) "?$queryId=$remoteNodeId" else ""
    navigate(route)
}

fun NavGraphBuilder.settingsScreen(
    navigateToEditRemoteNode: (Long?) -> Unit,
) {
    composable(route = settingsNavRoute) {
        SettingsRoute(
            navigateToEditRemoteNode = navigateToEditRemoteNode,
        )
    }
}

fun NavGraphBuilder.editRemoteNodeDialog(
    onBackClick: () -> Unit,
) {
    dialog(
        route = "$settingsRemoteNodeNavRoute?$queryId={$queryId}",
        arguments = listOf(
            navArgument(queryId) {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        val arguments = requireNotNull(it.arguments)
        val remoteNodeId = arguments.getString(queryId)?.toLongOrNull()
        EditRemoteNodeRoute(
            remoteNodeId = remoteNodeId,
            onBackClick = onBackClick,
        )
    }
}
