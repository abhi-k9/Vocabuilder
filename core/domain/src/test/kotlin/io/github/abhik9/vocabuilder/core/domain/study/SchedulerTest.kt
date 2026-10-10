package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.TestData.NOW
import io.github.abhik9.vocabuilder.core.testing.TestData.TODAY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchedulerTest {
    private val new = WordProgress("gre:abate")

    private fun WordProgress.answer(knew: Boolean, today: java.time.LocalDate = TODAY) =
        Scheduler.answer(this, knew, today, NOW)

    @Test
    fun knowingANewWordSkipsTheFirstStages() {
        val progress = new.answer(knew = true)

        assertEquals(Scheduler.FIRST_SIGHT_KNOWN_STAGE, progress.stage)
        assertEquals(TODAY.plusDays(7), progress.dueDay)
        assertEquals(TODAY, progress.introducedDay)
        assertEquals(1, progress.reviewCount)
        assertEquals(NOW, progress.lastReviewed)
    }

    @Test
    fun missingAWordSendsItBackToStageZeroForToday() {
        val progress = new.answer(knew = false)

        assertEquals(0, progress.stage)
        assertEquals(TODAY, progress.dueDay)
        assertEquals(1, progress.lapseCount)
        assertEquals(MasteryState.LEARNING, progress.masteryState)
    }

    @Test
    fun knowingAMissedWordLaterMovesItToTomorrow() {
        val progress = new.answer(knew = false).answer(knew = true)

        assertEquals(1, progress.stage)
        assertEquals(TODAY.plusDays(1), progress.dueDay)
        assertEquals(2, progress.reviewCount)
        assertEquals(MasteryState.REVIEWING, progress.masteryState)
    }

    @Test
    fun eachDueReviewMovesUpAStageUntilMastered() {
        var progress = new.answer(knew = false).answer(knew = true)
        var day = TODAY
        val intervals = mutableListOf<Long>()
        repeat(6) {
            day = progress.dueDay!!
            val next = progress.answer(knew = true, today = day)
            intervals += java.time.temporal.ChronoUnit.DAYS.between(day, next.dueDay)
            progress = next
        }

        assertEquals(listOf(3L, 7L, 16L, 35L, 90L, 90L), intervals)
        assertEquals(Scheduler.MAX_STAGE, progress.stage)
        assertEquals(MasteryState.MASTERED, progress.masteryState)
    }

    @Test
    fun reviewingEarlyKeepsTheSchedule() {
        val scheduled = WordProgress("gre:abate", stage = 2, dueDay = TODAY.plusDays(2), reviewCount = 3)

        val progress = scheduled.answer(knew = true)

        assertEquals(2, progress.stage)
        assertEquals(TODAY.plusDays(2), progress.dueDay)
        assertEquals(4, progress.reviewCount)
    }

    @Test
    fun missingAWordEarlyStillResetsIt() {
        val mastered = WordProgress("gre:abate", stage = 5, dueDay = TODAY.plusDays(20), reviewCount = 8)

        val progress = mastered.answer(knew = false)

        assertEquals(0, progress.stage)
        assertEquals(TODAY, progress.dueDay)
    }

    @Test
    fun missingAKnownWordUnmarksIt() {
        val known = WordProgress("gre:abate", alreadyKnown = true)

        assertTrue(known.answer(knew = true).alreadyKnown)
        assertFalse(known.answer(knew = false).alreadyKnown)
    }

    @Test
    fun keepsStarsAndTheDayTheWordWasIntroduced() {
        val progress = WordProgress("gre:abate", starred = true, introducedDay = TODAY.minusDays(9), reviewCount = 1)

        val answered = progress.answer(knew = true)

        assertTrue(answered.starred)
        assertEquals(TODAY.minusDays(9), answered.introducedDay)
    }
}
