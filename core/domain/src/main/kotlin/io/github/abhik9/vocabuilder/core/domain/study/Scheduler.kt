package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.WordProgress
import java.time.Instant
import java.time.LocalDate

/**
 * Decides when a word comes back, Leitner style: each "knew it" moves the word up a stage, and
 * each stage waits longer before the next review. "Didn't know" sends it back to stage 0, to be
 * seen again in the same session.
 *
 * | Stage | Next review | State     |
 * |-------|-------------|-----------|
 * | 0     | same day    | learning  |
 * | 1     | 1 day       | reviewing |
 * | 2     | 3 days      | reviewing |
 * | 3     | 7 days      | reviewing |
 * | 4     | 16 days     | mastered  |
 * | 5     | 35 days     | mastered  |
 * | 6     | 90 days     | mastered  |
 */
object Scheduler {
    private val INTERVAL_DAYS = longArrayOf(0, 1, 3, 7, 16, 35, 90)

    val MAX_STAGE = INTERVAL_DAYS.lastIndex

    /**
     * Knowing a word the first time it's shown skips the first stages: there's no point in
     * reviewing tomorrow a word the learner already knew.
     */
    const val FIRST_SIGHT_KNOWN_STAGE = 3

    /** Days until a word at [stage] is reviewed again. */
    fun intervalDays(stage: Int): Long = INTERVAL_DAYS[stage.coerceIn(0, MAX_STAGE)]

    /** The progress after answering a card on [today], at [now]. */
    fun answer(progress: WordProgress, knew: Boolean, today: LocalDate, now: Instant): WordProgress {
        val reviewedEarly = !progress.isNew && progress.stage > 0 && progress.dueDay?.isAfter(today) == true
        val stage = when {
            !knew -> 0

            progress.isNew -> FIRST_SIGHT_KNOWN_STAGE

            // Reviewing a word before it's due doesn't speed up its schedule.
            reviewedEarly -> progress.stage

            else -> (progress.stage + 1).coerceAtMost(MAX_STAGE)
        }
        return progress.copy(
            stage = stage,
            dueDay = if (knew && reviewedEarly) progress.dueDay else today.plusDays(intervalDays(stage)),
            introducedDay = progress.introducedDay ?: today,
            lastReviewed = now,
            reviewCount = progress.reviewCount + 1,
            lapseCount = progress.lapseCount + if (knew) 0 else 1,
            // Missing a word marked as known shows it isn't known after all.
            alreadyKnown = progress.alreadyKnown && knew,
        )
    }
}
