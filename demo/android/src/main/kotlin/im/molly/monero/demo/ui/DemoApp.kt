package im.molly.monero.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import im.molly.monero.demo.ui.navigation.*

@Composable
fun DemoApp(
    appState: DemoAppState = rememberDemoAppState(),
) {
    Background {
        Scaffold(
            bottomBar = {
                BottomBar(
                    destinations = appState.topLevelDestinations,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                    currentDestination = appState.currentDestination,
                    modifier = Modifier,
                )
            },
        ) { padding ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    )
            ) {
                Column(Modifier.fillMaxSize()) {
                    NavGraph(
                        navController = appState.navController,
                        onBackClick = appState::onBackClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun Background(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        content()
    }
}

@Composable
private fun BottomBar(
    destinations: List<TopLevelDestination>,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        destinations.forEach { destination ->
            val selected = currentDestination.isTopLevelDestinationInHierarchy(destination)
            val icon = if (selected) destination.selectedIcon else destination.unselectedIcon
            val text = stringResource(destination.iconTextRes)
            NavigationBarItem(
                onClick = { onNavigateToDestination(destination) },
                selected = selected,
                icon = { Icon(imageVector = icon, contentDescription = text) },
                label = { Text(text) },
                alwaysShowLabel = true,
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestinationInHierarchy(destination: TopLevelDestination): Boolean {
    return this?.hierarchy?.any {
        it.route?.startsWith(destination.name, true) ?: false
    } ?: false
}
