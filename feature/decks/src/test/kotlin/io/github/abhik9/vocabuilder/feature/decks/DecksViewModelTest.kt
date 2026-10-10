package io.github.abhik9.vocabuilder.feature.decks

import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveDeckOverviewsUseCase
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.MasteryCounts
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DecksViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val words = FakeWordRepository.withProgress()

    @Test
    fun groupsDecksByPackAndLevel() = runTest {
        words.progressRepository.setProgress(WordProgress("gre:abate", alreadyKnown = true))
        val viewModel = DecksViewModel(words, ObserveDeckOverviewsUseCase(words, TestData.clock))

        val state = viewModel.uiState.filterIsInstance<DecksUiState.Success>().first()

        val pack = state.packs.single()
        assertEquals(TestData.pack, pack.pack)
        assertEquals(MasteryCounts(new = 5, mastered = 1), pack.counts)
        assertEquals(listOf(Level.COMMON, Level.ADVANCED), pack.levels.map { it.level })
        assertEquals(listOf(TestData.commonDeck), pack.levels.first().decks.map { it.deck })
    }

    @Test
    fun deckViewModelListsTheDecksWordsInOrder() = runTest {
        val viewModel = DeckViewModel(
            deckId = TestData.advancedDeck.id,
            wordRepository = words,
            observeDeckOverviews = ObserveDeckOverviewsUseCase(words, TestData.clock),
            progressRepository = words.progressRepository,
        )

        val state = assertIs<DeckUiState.Success>(viewModel.uiState.first { it !is DeckUiState.Loading })

        assertEquals(listOf("pique", "vex"), state.words.map { it.headword })
        assertEquals(TestData.advancedDeck, state.overview.deck)
    }

    @Test
    fun deckViewModelReportsMissingDecks() = runTest {
        val viewModel = DeckViewModel(
            deckId = "gre/missing",
            wordRepository = words,
            observeDeckOverviews = ObserveDeckOverviewsUseCase(words, TestData.clock),
            progressRepository = words.progressRepository,
        )

        assertEquals(DeckUiState.NotFound, viewModel.uiState.first { it !is DeckUiState.Loading })
    }
}
