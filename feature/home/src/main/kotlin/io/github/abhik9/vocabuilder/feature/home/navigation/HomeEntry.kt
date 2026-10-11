package io.github.abhik9.vocabuilder.feature.home.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.navigation.DecksNavKey
import io.github.abhik9.vocabuilder.core.navigation.HomeNavKey
import io.github.abhik9.vocabuilder.core.navigation.Navigator
import io.github.abhik9.vocabuilder.core.navigation.StudyNavKey
import io.github.abhik9.vocabuilder.feature.home.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> {
        HomeRoute(
            onStartDailyReview = { navigator.navigate(StudyNavKey(StudyMode.DAILY_REVIEW)) },
            onStudyDeck = { deck -> navigator.navigate(StudyNavKey(StudyMode.DECK, deck.id)) },
            onOpenDecks = { navigator.navigate(DecksNavKey) },
            viewModel = hiltViewModel(),
        )
    }
}
