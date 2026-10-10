package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.model.Review
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.WordProgress
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Records an answer to a card: moves the word along its schedule and logs the review. */
class AnswerCardUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(wordId: String, knew: Boolean, mode: StudyMode): SessionAnswer {
        val before = progressRepository.getProgress(wordId)
        val now = Instant.now(clock)
        val after = Scheduler.answer(
            progress = before ?: WordProgress(wordId),
            knew = knew,
            today = LocalDate.now(clock),
            now = now,
        )
        val review = Review(
            wordId = wordId,
            reviewedAt = now,
            knew = knew,
            mode = mode,
            stageBefore = before?.stage ?: 0,
            stageAfter = after.stage,
        )
        val reviewId = progressRepository.recordAnswer(after, review)
        return SessionAnswer(wordId = wordId, knew = knew, before = before, after = after, reviewId = reviewId)
    }
}

/** Takes back an answer recorded by [AnswerCardUseCase]. */
class UndoAnswerUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(answer: SessionAnswer) {
        // Restore the progress from before the answer, but keep the word's star as it is now.
        val current = progressRepository.getProgress(answer.wordId)
        val restored = answer.before?.copy(
            starred = current?.starred ?: answer.before.starred,
        ) ?: current?.takeIf { it.starred }?.let { WordProgress(wordId = answer.wordId, starred = true) }
        progressRepository.undoAnswer(answer.wordId, restored, answer.reviewId)
    }
}
