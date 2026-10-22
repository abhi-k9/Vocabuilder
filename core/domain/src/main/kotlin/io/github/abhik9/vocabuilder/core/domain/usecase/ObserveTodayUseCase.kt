package io.github.abhik9.vocabuilder.core.domain.usecase

import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.domain.study.BuildStudyPlanUseCase
import io.github.abhik9.vocabuilder.core.domain.study.isNewToStudy
import io.github.abhik9.vocabuilder.core.domain.study.newWordsAllowedToday
import io.github.abhik9.vocabuilder.core.model.MasteryCounts
import io.github.abhik9.vocabuilder.core.model.TodayOverview
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Where the learner stands today, for the home screen. */
class ObserveTodayUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
) {
    // Built anew for each collection, so a screen shown again after midnight counts the new day.
    operator fun invoke(): Flow<TodayOverview> = flow {
        val today = LocalDate.now(clock)
        val startOfDay = today.atStartOfDay(clock.zone).toInstant()
        // Worked out as the answers change, not each time a deck, a word or a setting does.
        val answerCounts = progressRepository.observeAnswers().map { answers ->
            AnswerCounts(
                today = answers.count { !it.at.isBefore(startOfDay) },
                streak = currentStreak(answers.mapTo(HashSet()) { it.day(clock.zone) }, today),
            )
        }
        val todayOverviews = combine(
            wordRepository.observeDecks(),
            wordRepository.observeWordSummaries(),
            settingsRepository.settings,
            answerCounts,
        ) { decks, words, settings, answers ->
            val overviews = deckOverviews(decks, words, today)
            val newLeft = words.count { it.progress.isNewToStudy }
            val due = words.count { it.progress.isDue(today) }
            TodayOverview(
                dueCount = due.coerceAtMost(BuildStudyPlanUseCase.MAX_DAILY_REVIEWS),
                newCount = newWordsAllowedToday(words, settings.newWordsPerDay, today).coerceAtMost(newLeft),
                reviewedToday = answers.today,
                counts = MasteryCounts.of(words.map { it.progress.masteryState }),
                nextDeck = overviews.firstOrNull { !it.isCaughtUp },
                starredCount = words.count { it.progress.starred },
                troubleCount = words.count { it.progress.isTrouble }
                    .coerceAtMost(BuildStudyPlanUseCase.MAX_TROUBLE_WORDS),
                streak = answers.streak,
            )
        }
        emitAll(todayOverviews)
    }

    private data class AnswerCounts(val today: Int, val streak: Int)
}
