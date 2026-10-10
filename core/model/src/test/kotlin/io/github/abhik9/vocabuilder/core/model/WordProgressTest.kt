package io.github.abhik9.vocabuilder.core.model

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WordProgressTest {
    private val today = LocalDate.of(2026, 10, 10)
    private val studied = WordProgress(wordId = "gre:abate", reviewCount = 1, dueDay = today)

    @Test
    fun masteryStateFollowsTheStage() {
        assertEquals(MasteryState.NEW, WordProgress("gre:abate").masteryState)
        assertEquals(MasteryState.LEARNING, studied.copy(stage = 0).masteryState)
        assertEquals(MasteryState.REVIEWING, studied.copy(stage = 1).masteryState)
        assertEquals(MasteryState.REVIEWING, studied.copy(stage = WordProgress.MASTERED_STAGE - 1).masteryState)
        assertEquals(MasteryState.MASTERED, studied.copy(stage = WordProgress.MASTERED_STAGE).masteryState)
    }

    @Test
    fun knownWordsCountAsMastered() {
        assertEquals(MasteryState.MASTERED, WordProgress("gre:abate", alreadyKnown = true).masteryState)
    }

    @Test
    fun dueOnceItsDayComes() {
        assertTrue(studied.isDue(today))
        assertTrue(studied.copy(dueDay = today.minusDays(3)).isDue(today))
        assertFalse(studied.copy(dueDay = today.plusDays(1)).isDue(today))
    }

    @Test
    fun newAndKnownWordsAreNeverDue() {
        assertFalse(WordProgress("gre:abate", dueDay = today).isDue(today))
        assertFalse(studied.copy(alreadyKnown = true).isDue(today))
    }
}
