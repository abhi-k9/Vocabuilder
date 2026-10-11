package io.github.abhik9.vocabuilder.feature.settings.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.navigation.SettingsNavKey
import io.github.abhik9.vocabuilder.feature.settings.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntry() {
    entry<SettingsNavKey> { SettingsRoute(viewModel = hiltViewModel()) }
}
