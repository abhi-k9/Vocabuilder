package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.StudyMode
import io.github.abhik9.vocabuilder.core.model.WordSummary
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.first

/** Picks the cards of a study session. */
class BuildStudyPlanUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(request: StudyRequest, random: Random = Random.Default): StudyPlan {
        val words = wordRepository.observeWordSummaries().first()
        val decks = wordRepository.observeDecks().first()
        val settings = settingsRepository.settings.first()
        val today = LocalDate.now(clock)
        val ordered = words.sortedWith(deckOrder(decks))

        val ids = when (request.mode) {
            StudyMode.DECK -> {
                val deckWords = ordered.filter { it.deckId == request.deckId && !it.progress.alreadyKnown }
                if (request.practice) {
                    deckWords
                } else {
                    // Words being learned and due reviews come first, then the deck's new words.
                    val (seen, new) = deckWords.filter { it.isStudyable(today) }.partition { !it.progress.isNew }
                    seen.sortedBy { it.progress.dueDay } + new
                }.maybeShuffled(settings.shuffleCards, random)
            }

            StudyMode.DAILY_REVIEW -> {
                // The most overdue words first, then mixed so cards from one deck don't come in a row.
                val due = ordered.filter { it.progress.isDue(today) }
                    .sortedBy { it.progress.dueDay }
                    .take(MAX_DAILY_REVIEWS)
                    .shuffled(random)
                val introducedToday = words.count { it.progress.introducedDay == today }
                val newAllowed = (settings.newWordsPerDay - introducedToday).coerceAtLeast(0)
                val new = ordered.filter { it.progress.isNew && !it.progress.alreadyKnown }.take(newAllowed)
                due + new
            }

            StudyMode.STARRED -> ordered.filter { it.progress.starred }.maybeShuffled(settings.shuffleCards, random)
        }
        return StudyPlan(request = request, wordIds = ids.map { it.id }, isPractice = request.practice)
    }

    private fun WordSummary.isStudyable(today: LocalDate) = progress.isNew || progress.isDue(today)

    private fun List<WordSummary>.maybeShuffled(shuffle: Boolean, random: Random) =
        if (shuffle) shuffled(random) else this

    private fun deckOrder(decks: List<Deck>): Comparator<WordSummary> {
        val deckIndex = decks.withIndex().associate { (index, deck) -> deck.id to index }
        return compareBy<WordSummary> { deckIndex[it.deckId] ?: Int.MAX_VALUE }.thenBy { it.position }
    }

    companion object {
        /** Reviews in one daily session, so a long break doesn't make for an endless one. */
        const val MAX_DAILY_REVIEWS = 100
    }
}
