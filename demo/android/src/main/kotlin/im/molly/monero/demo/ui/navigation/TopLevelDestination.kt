package im.molly.monero.demo.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import im.molly.monero.demo.R
import im.molly.monero.demo.ui.theme.AppIcons

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextRes: Int,
) {
    HOME(
        selectedIcon = AppIcons.Home,
        unselectedIcon = AppIcons.HomeOutlined,
        iconTextRes = R.string.home,
    ),
    HISTORY(
        selectedIcon = AppIcons.History,
        unselectedIcon = AppIcons.HistoryOutlined,
        iconTextRes = R.string.history,
    ),
    SETTINGS(
        selectedIcon = AppIcons.History,
        unselectedIcon = AppIcons.HistoryOutlined,
        iconTextRes = R.string.settings,
    )
}
