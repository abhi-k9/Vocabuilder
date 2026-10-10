package io.github.abhik9.vocabuilder.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.R
import io.github.abhik9.vocabuilder.core.navigation.DecksNavKey

/** The screens reachable from the navigation bar. */
enum class TopLevelDestination(
    val key: NavKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val label: Int,
) {
    DECKS(DecksNavKey, Icons.Filled.Style, Icons.Outlined.Style, R.string.nav_decks),
}
