package io.github.abhik9.vocabuilder.core.domain.usecase

import io.github.abhik9.vocabuilder.core.model.MasteryCounts
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.TestData.TODAY
import io.github.abhik9.vocabuilder.core.testing.repository.FakeUserSettingsRepository
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ObserveTodayUseCaseTest {
    private val words = FakeWordRepository.withProgress()
    private val progress = words.progressRepository
    private val settings = FakeUserSettingsRepository(UserSettings(newWordsPerDay = 3))

    @Test
    fun summarizesTodayAcrossDecks() = runTest {
        progress.setProgress(
            WordProgress("gre:abate", stage = 4, dueDay = TODAY.plusDays(10), reviewCount = 5),
            WordProgress("gre:laud", stage = 1, dueDay = TODAY, reviewCount = 2, introducedDay = TODAY),
            WordProgress("gre:wane", stage = 0, dueDay = TODAY, reviewCount = 3, lapseCount = 2),
            WordProgress("gre:zeal", alreadyKnown = true, starred = true),
        )
        progress.recordAnswer(
            progress.getProgress("gre:laud")!!,
            Review("gre:laud", TestData.NOW, knew = true, mode = StudyMode.DECK, stageBefore = 0, stageAfter = 1),
        )

        val today = ObserveTodayUseCase(words, progress, settings, TestData.clock)().first()

        assertEquals(2, today.dueCount)
        // Three new words a day, one introduced today, and two new words left.
        assertEquals(2, today.newCount)
        assertEquals(1, today.reviewedToday)
        assertEquals(MasteryCounts(new = 2, learning = 1, reviewing = 1, mastered = 2), today.counts)
        assertEquals(TestData.commonDeck, today.nextDeck?.deck)
        assertEquals(1, today.starredCount)
        assertEquals(1, today.troubleCount)
    }

    @Test
    fun deckOverviewsCountStatesAndDueWords() = runTest {
        progress.setProgress(WordProgress("gre:pique", stage = 1, dueDay = TODAY, reviewCount = 1))

        val overviews = ObserveDeckOverviewsUseCase(words, TestData.clock)().first()

        assertEquals(listOf(TestData.commonDeck, TestData.advancedDeck), overviews.map { it.deck })
        assertEquals(MasteryCounts(new = 1, reviewing = 1), overviews[1].counts)
        assertEquals(1, overviews[1].dueCount)
        assertEquals(MasteryCounts(new = 4), overviews[0].counts)
    }
}
