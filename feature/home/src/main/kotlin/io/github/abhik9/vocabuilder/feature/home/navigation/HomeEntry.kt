package io.github.abhik9.vocabuilder.feature.home.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.navigation.DecksNavKey
import io.github.abhik9.vocabuilder.core.navigation.HistoryNavKey
import io.github.abhik9.vocabuilder.core.navigation.HomeNavKey
import io.github.abhik9.vocabuilder.core.navigation.Navigator
import io.github.abhik9.vocabuilder.core.navigation.StudyNavKey
import io.github.abhik9.vocabuilder.core.navigation.WordNavKey
import io.github.abhik9.vocabuilder.feature.home.HistoryRoute
import io.github.abhik9.vocabuilder.feature.home.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> {
        HomeRoute(
            onStartDailyReview = { quiz -> navigator.navigate(StudyNavKey(StudyMode.DAILY_REVIEW, quiz = quiz)) },
            onStudyDeck = { deck -> navigator.navigate(StudyNavKey(StudyMode.DECK, deck.id)) },
            onOpenDecks = { navigator.navigate(DecksNavKey) },
            onStudyStarred = { navigator.navigate(StudyNavKey(StudyMode.STARRED)) },
            onStudyTrouble = { navigator.navigate(StudyNavKey(StudyMode.TROUBLE)) },
            onOpenHistory = { navigator.navigate(HistoryNavKey) },
            viewModel = hiltViewModel(),
        )
    }
    entry<HistoryNavKey> {
        HistoryRoute(
            onBack = { navigator.goBack() },
            onOpenWord = { wordId -> navigator.navigate(WordNavKey(wordId)) },
            onStudyTrouble = { navigator.navigate(StudyNavKey(StudyMode.TROUBLE)) },
            viewModel = hiltViewModel(),
        )
    }
}
