package io.github.abhik9.vocabuilder.core.domain.study

import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.model.WordSummary
import java.time.LocalDate

/** Never studied, nor marked as known: a word the daily review can bring in. */
internal val WordProgress.isNewToStudy: Boolean get() = isNew && !alreadyKnown

/** How many new words the daily review can still bring in today: the day's allowance, less those it brought in. */
internal fun newWordsAllowedToday(words: List<WordSummary>, newWordsPerDay: Int, today: LocalDate): Int =
    (newWordsPerDay - words.count { it.progress.introducedDay == today }).coerceAtLeast(0)
