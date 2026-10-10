package io.github.abhik9.vocabuilder.core.domain.usecase

import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.DeckOverview
import io.github.abhik9.vocabuilder.core.model.MasteryCounts
import io.github.abhik9.vocabuilder.core.model.WordSummary
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Every deck with the learner's progress through it, updated as they study. */
class ObserveDeckOverviewsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<DeckOverview>> = combine(
        wordRepository.observeDecks(),
        wordRepository.observeWordSummaries(),
    ) { decks, words -> deckOverviews(decks, words, LocalDate.now(clock)) }
}

internal fun deckOverviews(decks: List<Deck>, words: List<WordSummary>, today: LocalDate): List<DeckOverview> {
    val wordsByDeck = words.groupBy { it.deckId }
    return decks.map { deck ->
        val deckWords = wordsByDeck[deck.id].orEmpty()
        DeckOverview(
            deck = deck,
            counts = MasteryCounts.of(deckWords.map { it.progress.masteryState }),
            dueCount = deckWords.count { it.progress.isDue(today) },
        )
    }
}
