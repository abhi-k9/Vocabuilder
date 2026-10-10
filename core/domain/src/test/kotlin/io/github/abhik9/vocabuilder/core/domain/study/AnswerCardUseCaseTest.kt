package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.testing.TestData
import io.github.abhik9.vocabuilder.core.testing.TestData.TODAY
import io.github.abhik9.vocabuilder.core.testing.repository.FakeProgressRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AnswerCardUseCaseTest {
    private val progress = FakeProgressRepository()
    private val answerCard = AnswerCardUseCase(progress, TestData.clock)
    private val undoAnswer = UndoAnswerUseCase(progress)

    @Test
    fun recordsTheNewProgressAndTheReview() = runTest {
        val answer = answerCard("gre:abate", knew = false, mode = StudyMode.DECK)

        assertNull(answer.before)
        assertEquals(WordProgress("gre:abate").let { Scheduler.answer(it, false, TODAY, TestData.NOW) }, answer.after)
        assertEquals(answer.after, progress.getProgress("gre:abate"))
        val review = progress.reviews.value.single()
        assertEquals(answer.reviewId, review.id)
        assertEquals(StudyMode.DECK, review.mode)
        assertEquals(false, review.knew)
        assertEquals(TestData.NOW, review.reviewedAt)
    }

    @Test
    fun undoRestoresTheProgressAndDeletesTheReview() = runTest {
        val before = WordProgress("gre:abate", stage = 2, dueDay = TODAY, reviewCount = 4)
        progress.setProgress(before)
        val answer = answerCard("gre:abate", knew = true, mode = StudyMode.DAILY_REVIEW)

        undoAnswer(answer)

        assertEquals(before, progress.getProgress("gre:abate"))
        assertTrue(progress.reviews.value.isEmpty())
    }

    @Test
    fun undoingAFirstAnswerForgetsTheWordButKeepsItsStar() = runTest {
        val answer = answerCard("gre:abate", knew = true, mode = StudyMode.DECK)
        progress.setStarred("gre:abate", true)

        undoAnswer(answer)

        assertEquals(WordProgress("gre:abate", starred = true), progress.getProgress("gre:abate"))
    }

    @Test
    fun undoingAFirstAnswerLeavesNoProgress() = runTest {
        val answer = answerCard("gre:abate", knew = true, mode = StudyMode.DECK)

        undoAnswer(answer)

        assertNull(progress.getProgress("gre:abate"))
    }
}
