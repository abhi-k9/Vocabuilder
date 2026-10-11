package io.github.abhik9.vocabuilder.feature.words

import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class WordViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val words = FakeWordRepository.withProgress()

    @Test
    fun browserSearchesAndFilters() = runTest {
        words.progressRepository.setProgress(WordProgress("gre:vex", starred = true))
        val viewModel = WordsViewModel(words)
        backgroundScope.launch { viewModel.uiState.collect {} }

        var state = assertIs<WordsUiState.Success>(viewModel.uiState.first { it is WordsUiState.Success })
        assertEquals(TestData.words.map { it.headword }.sorted(), state.words.map { it.headword })

        viewModel.setFilter(WordFilter.STARRED)
        state = assertIs<WordsUiState.Success>(
            viewModel.uiState.first {
                (it as? WordsUiState.Success)?.filter == WordFilter.STARRED
            },
        )
        assertEquals(listOf("vex"), state.words.map { it.headword })
    }

    @Test
    fun wordPageShowsTheWordAndTogglesItsStar() = runTest {
        val viewModel = WordViewModel("gre:abate", words, words.progressRepository, TestData.clock)
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = assertIs<WordUiState.Success>(viewModel.uiState.first { it is WordUiState.Success })
        assertEquals("abate", state.word.word.headword)
        assertEquals(TestData.commonDeck, state.deck)

        viewModel.toggleStar()
        viewModel.toggleKnown()

        assertEquals(
            WordProgress("gre:abate", starred = true, alreadyKnown = true),
            words.progressRepository.getProgress("gre:abate"),
        )
    }

    @Test
    fun missingWordsAreReported() = runTest {
        val viewModel = WordViewModel("gre:missing", words, words.progressRepository, TestData.clock)

        assertEquals(WordUiState.NotFound, viewModel.uiState.first { it != WordUiState.Loading })
    }
}
