package io.github.abhik9.vocabuilder.feature.decks.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.navigation.DeckNavKey
import io.github.abhik9.vocabuilder.core.navigation.DecksNavKey
import io.github.abhik9.vocabuilder.core.navigation.Navigator
import io.github.abhik9.vocabuilder.core.navigation.StudyNavKey
import io.github.abhik9.vocabuilder.core.navigation.WordNavKey
import io.github.abhik9.vocabuilder.feature.decks.DeckRoute
import io.github.abhik9.vocabuilder.feature.decks.DeckViewModel
import io.github.abhik9.vocabuilder.feature.decks.DecksRoute

fun EntryProviderScope<NavKey>.decksEntries(navigator: Navigator) {
    entry<DecksNavKey> {
        DecksRoute(
            onDeckClick = { deck -> navigator.navigate(DeckNavKey(deck.id)) },
            viewModel = hiltViewModel(),
        )
    }
    entry<DeckNavKey> { key ->
        DeckRoute(
            onBack = { navigator.goBack() },
            onStudy = { practice, quiz ->
                navigator.navigate(StudyNavKey(StudyMode.DECK, key.deckId, practice = practice, quiz = quiz))
            },
            onWordClick = { word -> navigator.navigate(WordNavKey(word.id)) },
            viewModel = hiltViewModel<DeckViewModel, DeckViewModel.Factory> { factory -> factory.create(key.deckId) },
        )
    }
}
