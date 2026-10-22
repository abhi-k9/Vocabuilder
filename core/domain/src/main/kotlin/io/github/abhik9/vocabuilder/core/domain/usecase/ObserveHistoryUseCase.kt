package io.github.abhik9.vocabuilder.core.domain.usecase

import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.Answer
import io.github.abhik9.vocabuilder.core.model.DayActivity
import io.github.abhik9.vocabuilder.core.model.History
import io.github.abhik9.vocabuilder.core.model.WordSummary
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** The learner's history of answers, for the history screen. */
class ObserveHistoryUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val clock: Clock,
) {
    // Built anew for each collection, so a screen shown again after midnight counts the new day.
    operator fun invoke(): Flow<History> = flow {
        val today = LocalDate.now(clock)
        val histories = combine(progressRepository.observeAnswers(), wordRepository.observeWordSummaries()) {
                answers,
                words,
            ->
            history(answers, words, today, clock.zone)
        }
        emitAll(histories)
    }
}

internal fun history(answers: List<Answer>, words: List<WordSummary>, today: LocalDate, zone: ZoneId): History {
    val byDay = answers.groupBy { it.at.atZone(zone).toLocalDate() }
    val recentDays = (History.RECENT_DAYS - 1 downTo 0).map { daysAgo ->
        val day = today.minusDays(daysAgo.toLong())
        val dayAnswers = byDay[day].orEmpty()
        DayActivity(day, answers = dayAnswers.size, knew = dayAnswers.count { it.knew })
    }
    return History(
        recentDays = recentDays,
        currentStreak = currentStreak(byDay.keys, today),
        longestStreak = longestStreak(byDay.keys),
        totalAnswers = answers.size,
        studiedWords = words.count { !it.progress.isNew },
        mostMissed = words.filter { it.progress.lapseCount > 0 }
            .sortedWith(compareByDescending<WordSummary> { it.progress.lapseCount }.thenBy { it.headword })
            .take(History.MOST_MISSED),
    )
}

/** The days in a row with answers up to [today], or, while today has none yet, up to yesterday. */
internal fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
    val last = if (today in days) today else today.minusDays(1)
    return generateSequence(last) { it.minusDays(1) }.takeWhile { it in days }.count()
}

/** The most days in a row among [days]. */
internal fun longestStreak(days: Set<LocalDate>): Int {
    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    for (day in days.sorted()) {
        run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
        longest = maxOf(longest, run)
        previous = day
    }
    return longest
}
