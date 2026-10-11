package io.github.abhik9.vocabuilder.feature.home

import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.MainDispatcherRule
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.repository.FakeUserSettingsRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun showsTodaysReviewAndTheDeckToContinue() = runTest {
        val words = FakeWordRepository.withProgress()
        words.progressRepository.setProgress(
            WordProgress("gre:abate", stage = 1, dueDay = TestData.TODAY, reviewCount = 1),
        )
        val settings = FakeUserSettingsRepository(UserSettings(newWordsPerDay = 3))
        val viewModel = HomeViewModel(ObserveTodayUseCase(words, words.progressRepository, settings, TestData.clock))

        val today = viewModel.uiState.filterIsInstance<HomeUiState.Success>().first().today

        assertEquals(1, today.dueCount)
        assertEquals(3, today.newCount)
        assertEquals(4, today.dailyReviewCount)
        assertEquals(TestData.commonDeck, today.nextDeck?.deck)
    }
}
