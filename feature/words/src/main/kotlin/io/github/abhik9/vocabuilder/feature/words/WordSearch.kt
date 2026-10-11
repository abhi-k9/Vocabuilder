package io.github.abhik9.vocabuilder.feature.words

import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.WordSummary
import java.text.Normalizer

/** Which words the browser lists. */
enum class WordFilter {
    ALL,
    STARRED,
    NEW,
    LEARNING,
    REVIEWING,
    MASTERED,
    ;

    fun matches(word: WordSummary): Boolean = when (this) {
        ALL -> true
        STARRED -> word.progress.starred
        NEW -> word.progress.masteryState == MasteryState.NEW
        LEARNING -> word.progress.masteryState == MasteryState.LEARNING
        REVIEWING -> word.progress.masteryState == MasteryState.REVIEWING
        MASTERED -> word.progress.masteryState == MasteryState.MASTERED
    }
}

/**
 * The words matching [filter] and [query], ignoring case and accents. Words whose headword starts
 * with the query come first, then those containing it, then those whose meaning contains it; each
 * group in alphabetical order.
 */
internal fun searchWords(words: List<WordSummary>, query: String, filter: WordFilter): List<WordSummary> {
    val filtered = words.filter(filter::matches)
    val needle = normalize(query.trim())
    if (needle.isEmpty()) return filtered.sortedBy { normalize(it.headword) }
    return filtered
        .mapNotNull { word ->
            val headword = normalize(word.headword)
            val rank = when {
                headword.startsWith(needle) -> 0
                needle in headword -> 1
                needle in normalize(word.definition) -> 2
                else -> return@mapNotNull null
            }
            Triple(rank, headword, word)
        }
        .sortedWith(compareBy({ it.first }, { it.second }))
        .map { it.third }
}

internal fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(DIACRITICS, "")
    .lowercase()

private val DIACRITICS = Regex("\\p{M}+")
