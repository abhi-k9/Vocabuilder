package io.github.abhik9.vocabuilder.core.domain.usecase

import io.github.abhik9.vocabuilder.core.model.DayActivity
import io.github.abhik9.vocabuilder.core.model.History
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.TestData.TODAY
import io.github.abhik9.vocabuilder.core.testing.repository.FakeWordRepository
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ObserveHistoryUseCaseTest {
    private val words = FakeWordRepository.withProgress()
    private val progress = words.progressRepository

    private suspend fun answer(day: LocalDate, knew: Boolean, wordId: String = "gre:abate") {
        val at = day.atTime(9, 0).toInstant(ZoneOffset.UTC)
        progress.recordAnswer(
            progress.getProgress(wordId) ?: WordProgress(wordId),
            Review(wordId, at, knew, StudyMode.DECK, stageBefore = 0, stageAfter = 0),
        )
    }

    @Test
    fun countsTheLastFourWeeksByDayAndTheMostMissedWords() = runTest {
        answer(TODAY.minusDays(40), knew = true)
        answer(TODAY.minusDays(1), knew = false)
        answer(TODAY, knew = true)
        answer(TODAY, knew = false, wordId = "gre:vex")
        progress.setProgress(
            WordProgress("gre:abate", reviewCount = 3, lapseCount = 1),
            WordProgress("gre:vex", reviewCount = 4, lapseCount = 3),
            WordProgress("gre:zeal", starred = true),
        )

        val history = ObserveHistoryUseCase(words, progress, TestData.clock)().first()

        assertEquals(History.RECENT_DAYS, history.recentDays.size)
        assertEquals(DayActivity(TODAY, answers = 2, knew = 1), history.recentDays.last())
        assertEquals(
            DayActivity(TODAY.minusDays(1), answers = 1, knew = 0),
            history.recentDays[History.RECENT_DAYS - 2],
        )
        assertEquals(TODAY.minusDays(History.RECENT_DAYS - 1L), history.recentDays.first().day)
        assertEquals(3, history.recentAnswers)
        assertEquals(1, history.recentKnew)
        assertEquals(4, history.totalAnswers)
        assertEquals(2, history.studiedWords)
        assertEquals(listOf("vex", "abate"), history.mostMissed.map { it.headword })
        assertEquals(2, history.currentStreak)
    }

    @Test
    fun aStreakLastsUntilADayEndsWithoutAnswers() {
        val days = setOf(TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(5))

        assertEquals(2, currentStreak(days, TODAY))
        assertEquals(3, currentStreak(days + TODAY, TODAY))
        assertEquals(0, currentStreak(days, TODAY.plusDays(1)))
        assertEquals(0, currentStreak(emptySet(), TODAY))
    }

    @Test
    fun findsTheLongestStreak() {
        val days = setOf(1, 2, 3, 5, 6, 10, 11, 12, 13).map { TODAY.minusDays(it.toLong()) }.toSet()

        assertEquals(4, longestStreak(days))
        assertEquals(0, longestStreak(emptySet()))
    }
}
