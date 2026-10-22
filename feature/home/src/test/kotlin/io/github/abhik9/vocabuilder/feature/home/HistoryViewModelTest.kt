package io.github.abhik9.vocabuilder.feature.home

import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveHistoryUseCase
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun showsTheStreakAndTheMostMissedWords() = runTest {
        val words = FakeWordRepository.withProgress()
        val progress = words.progressRepository
        progress.recordAnswer(
            WordProgress("gre:vex", reviewCount = 2, lapseCount = 2),
            Review("gre:vex", TestData.NOW, knew = false, mode = StudyMode.DECK, stageBefore = 0, stageAfter = 0),
        )
        val viewModel = HistoryViewModel(ObserveHistoryUseCase(words, progress, TestData.clock))

        val history = viewModel.uiState.filterIsInstance<HistoryUiState.Success>().first().history

        assertEquals(1, history.currentStreak)
        assertEquals(1, history.totalAnswers)
        assertEquals(listOf("vex"), history.mostMissed.map { it.headword })
    }
}
