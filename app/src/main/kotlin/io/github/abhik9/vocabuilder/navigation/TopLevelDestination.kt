package io.github.abhik9.vocabuilder.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.R
import io.github.abhik9.vocabuilder.core.navigation.DecksNavKey
import io.github.abhik9.vocabuilder.core.navigation.HomeNavKey
import io.github.abhik9.vocabuilder.core.navigation.SettingsNavKey
import io.github.abhik9.vocabuilder.core.navigation.WordsNavKey

/** The screens reachable from the navigation bar. */
enum class TopLevelDestination(
    val key: NavKey,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @param:StringRes val label: Int,
) {
    HOME(HomeNavKey, Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    DECKS(DecksNavKey, Icons.Filled.Style, Icons.Outlined.Style, R.string.nav_decks),
    WORDS(WordsNavKey, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, R.string.nav_words),
    SETTINGS(SettingsNavKey, Icons.Filled.Settings, Icons.Outlined.Settings, R.string.nav_settings),
}
