package io.github.abhik9.vocabuilder.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.abhik9.vocabuilder.core.navigation.Navigator
import io.github.abhik9.vocabuilder.feature.decks.navigation.decksEntries
import io.github.abhik9.vocabuilder.feature.study.navigation.studyEntry
import io.github.abhik9.vocabuilder.navigation.TopLevelDestination

/**
 * The app shell: an adaptive navigation bar (or rail on wide screens) around the screen on top
 * of the back stack. The bar is hidden on detail screens such as a study session.
 */
@Composable
fun VocabuilderApp() {
    val backStack = rememberNavBackStack(Navigator.ROOT)
    val navigator = remember(backStack) { Navigator(backStack) }

    val currentKey = backStack.lastOrNull() ?: Navigator.ROOT
    val currentTopLevelKey = navigator.currentTopLevelKey
    val layoutType = if (Navigator.isTopLevel(currentKey)) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    } else {
        NavigationSuiteType.None
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination.key == currentTopLevelKey
                item(
                    selected = selected,
                    onClick = { navigator.navigate(destination.key) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
        layoutType = layoutType,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                decksEntries(navigator)
                studyEntry(navigator)
            },
        )
    }
}
