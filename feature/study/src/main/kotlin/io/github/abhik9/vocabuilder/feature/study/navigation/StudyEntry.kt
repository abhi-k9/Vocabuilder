package io.github.abhik9.vocabuilder.feature.study.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.domain.study.StudyRequest
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.navigation.Navigator
import io.github.abhik9.vocabuilder.core.navigation.StudyNavKey
import io.github.abhik9.vocabuilder.feature.study.StudyRoute
import io.github.abhik9.vocabuilder.feature.study.StudyViewModel

fun EntryProviderScope<NavKey>.studyEntry(navigator: Navigator) {
    entry<StudyNavKey> { key ->
        StudyRoute(
            onClose = { navigator.goBack() },
            onPractice = { navigator.replace(key.copy(practice = true)) },
            onNextDeck = { deck -> navigator.replace(StudyNavKey(StudyMode.DECK, deck.id, quiz = key.quiz)) },
            viewModel = hiltViewModel<StudyViewModel, StudyViewModel.Factory> { factory ->
                factory.create(
                    StudyRequest(mode = key.mode, deckId = key.deckId, practice = key.practice, quiz = key.quiz),
                )
            },
        )
    }
}
