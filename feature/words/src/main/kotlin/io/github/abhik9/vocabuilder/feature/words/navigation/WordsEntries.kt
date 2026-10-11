package io.github.abhik9.vocabuilder.feature.words.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.navigation.Navigator
import io.github.abhik9.vocabuilder.core.navigation.WordNavKey
import io.github.abhik9.vocabuilder.core.navigation.WordsNavKey
import io.github.abhik9.vocabuilder.feature.words.WordRoute
import io.github.abhik9.vocabuilder.feature.words.WordViewModel
import io.github.abhik9.vocabuilder.feature.words.WordsRoute

fun EntryProviderScope<NavKey>.wordsEntries(navigator: Navigator) {
    entry<WordsNavKey> {
        WordsRoute(
            onWordClick = { word -> navigator.navigate(WordNavKey(word.id)) },
            viewModel = hiltViewModel(),
        )
    }
    entry<WordNavKey> { key ->
        WordRoute(
            onBack = { navigator.goBack() },
            onOpenWord = { wordId -> navigator.navigate(WordNavKey(wordId)) },
            viewModel = hiltViewModel<WordViewModel, WordViewModel.Factory> { factory -> factory.create(key.wordId) },
        )
    }
}
