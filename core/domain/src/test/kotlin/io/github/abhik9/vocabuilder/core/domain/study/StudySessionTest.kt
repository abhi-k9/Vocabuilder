package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.WordProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudySessionTest {
    private fun answer(wordId: String, knew: Boolean, stageAfter: Int = if (knew) 3 else 0) = SessionAnswer(
        wordId = wordId,
        knew = knew,
        before = null,
        after = WordProgress(wordId, stage = stageAfter, reviewCount = 1),
        reviewId = 0,
    )

    private fun StudySession.answer(knew: Boolean) = answer(answer(currentWordId!!, knew))

    @Test
    fun knownCardsLeaveTheQueue() {
        val session = StudySession(listOf("a", "b", "c")).answer(knew = true)

        assertEquals(listOf("b", "c"), session.queue)
        assertEquals(1, session.doneCount)
        assertEquals(3, session.wordCount)
    }

    @Test
    fun missedCardsComeBackAFewCardsLater() {
        val session = StudySession(listOf("a", "b", "c", "d", "e", "f", "g")).answer(knew = false)

        assertEquals(listOf("b", "c", "d", "e", "a", "f", "g"), session.queue)
        assertEquals(7, session.wordCount)
    }

    @Test
    fun missedCardsGoToTheEndOfAShortQueue() {
        val session = StudySession(listOf("a", "b")).answer(knew = false)

        assertEquals(listOf("b", "a"), session.queue)
    }

    @Test
    fun theSessionEndsWhenEveryCardIsKnown() {
        var session = StudySession(listOf("a"))
        session = session.answer(knew = false)
        assertEquals(listOf("a"), session.queue)
        session = session.answer(knew = true)

        assertTrue(session.isFinished)
        assertNull(session.currentWordId)
    }

    @Test
    fun undoRestoresTheQueueAndDropsTheAnswer() {
        val start = StudySession(listOf("a", "b", "c"))
        val answered = start.answer(knew = false)

        val undone = answered.undo()

        assertEquals(start.queue, undone.queue)
        assertFalse(undone.canUndo)
    }

    @Test
    fun undoWithoutAnswersDoesNothing() {
        val session = StudySession(listOf("a"))
        assertEquals(session, session.undo())
    }

    @Test
    fun rejectsAnswersForAnotherCard() {
        assertFailsWith<IllegalArgumentException> {
            StudySession(listOf("a", "b")).answer(answer("b", knew = true))
        }
    }

    @Test
    fun summarizesTheSession() {
        var session = StudySession(listOf("a", "b", "c"))
        session = session.answer(answer("a", knew = true, stageAfter = 4))
        session = session.answer(knew = false)
        session = session.answer(knew = true)
        session = session.answer(knew = true)

        assertTrue(session.isFinished)
        assertEquals(SessionSummary(studied = 3, knewFirstTime = 2, missed = 1, newlyMastered = 1), session.summary())
    }
}
