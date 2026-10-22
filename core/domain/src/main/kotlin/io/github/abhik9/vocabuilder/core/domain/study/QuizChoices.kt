package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.Word
import io.github.abhik9.vocabuilder.core.model.WordSummary
import kotlin.random.Random

/** One choice of a quiz card: a word of the app, shown by its word or by its main meaning. */
data class QuizChoice(
    val wordId: String,
    val headword: String,
    val definition: String,
)

object QuizChoices {
    const val COUNT = 4

    /**
     * The choices for [word]: itself and [COUNT] - 1 other words of [pool], in random order. The others are picked at
     * random from the words closest to it, so the right one doesn't stand out: first those of its deck with the same
     * part of speech, then those of any deck with it, then any. Its synonyms and words with the same meaning are left
     * out, as they would be right too.
     */
    fun of(word: Word, pool: List<WordSummary>, random: Random): List<QuizChoice> {
        val sense = word.primarySense
        // Its synonyms, and the word itself in another deck.
        val excluded = word.synonyms.mapTo(HashSet()) { it.lowercase() } + word.headword.lowercase()
        val candidates = pool.filter {
            it.id != word.id && it.headword.lowercase() !in excluded && it.definition != sense.definition
        }
        val (samePos, otherPos) = candidates.partition { it.partOfSpeech == sense.partOfSpeech }
        val (sameDeck, otherDecks) = samePos.partition { it.deckId == word.deckId }
        val others = listOf(sameDeck, otherDecks, otherPos)
            .flatMap { it.shuffled(random) }
            .distinctBy { it.definition }
            .take(COUNT - 1)
            .map { QuizChoice(it.id, it.headword, it.definition) }
        return (others + QuizChoice(word.id, word.headword, sense.definition)).shuffled(random)
    }
}
