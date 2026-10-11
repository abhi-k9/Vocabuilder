package io.github.abhik9.vocabuilder.feature.words

import io.github.abhik9.vocabuilder.core.model.DictionaryEntry
import io.github.abhik9.vocabuilder.core.model.DictionarySense
import io.github.abhik9.vocabuilder.core.model.DictionarySource
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.FakePronouncer
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.repository.FakeDictionaryRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class WordViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val words = FakeWordRepository.withProgress()
    private val pronouncer = FakePronouncer()
    private val subside = DictionaryEntry(
        word = "subside",
        senses = listOf(DictionarySense(PartOfSpeech.VERB, "wear off or die down", "The pain subsided")),
        source = DictionarySource.WORDNET,
    )

    private fun wordViewModel(wordId: String) = WordViewModel(
        wordId = wordId,
        wordRepository = words,
        dictionaryRepository = FakeDictionaryRepository(subside),
        progressRepository = words.progressRepository,
        pronouncer = pronouncer,
        clock = TestData.clock,
    )

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
        val viewModel = wordViewModel("gre:abate")
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = assertIs<WordUiState.Success>(viewModel.uiState.first { it is WordUiState.Success })
        assertEquals("abate", state.word.word.headword)
        assertEquals(TestData.commonDeck, state.deck)

        viewModel.pronounce()
        assertEquals(listOf("abate"), pronouncer.spoken)

        viewModel.toggleStar()
        viewModel.toggleKnown()

        assertEquals(
            WordProgress("gre:abate", starred = true, alreadyKnown = true),
            words.progressRepository.getProgress("gre:abate"),
        )
    }

    @Test
    fun wordPageLooksUpRelatedWordsInTheDictionary() = runTest {
        val viewModel = wordViewModel("gre:abate")

        viewModel.lookUp("subside")
        assertEquals(LookedUpWord("subside", subside), viewModel.lookedUp.value)
        viewModel.pronounceLookedUp()
        assertEquals(listOf("subside"), pronouncer.spoken)

        viewModel.lookUp("ebb")
        assertEquals(LookedUpWord("ebb", null), viewModel.lookedUp.value)

        viewModel.dismissLookedUp()
        assertNull(viewModel.lookedUp.value)
    }

    @Test
    fun missingWordsAreReported() = runTest {
        val viewModel = wordViewModel("gre:missing")

        assertEquals(WordUiState.NotFound, viewModel.uiState.first { it != WordUiState.Loading })
    }
}
