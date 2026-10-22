package io.github.abhik9.vocabuilder.core.navigation

import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.model.StudyMode
import kotlinx.serialization.Serializable

/** Today's review and progress, where the app starts. */
@Serializable
data object HomeNavKey : NavKey

@Serializable
data object DecksNavKey : NavKey

/** Every word, to search and filter. */
@Serializable
data object WordsNavKey : NavKey

/** A word's meanings and the learner's progress on it. */
@Serializable
data class WordNavKey(val wordId: String) : NavKey

@Serializable
data object SettingsNavKey : NavKey

/** A deck's progress and words. */
@Serializable
data class DeckNavKey(val deckId: String) : NavKey

/**
 * A study session.
 *
 * @property deckId the deck, for [StudyMode.DECK].
 * @property practice go through words that aren't due.
 */
@Serializable
data class StudyNavKey(
    val mode: StudyMode,
    val deckId: String? = null,
    val practice: Boolean = false,
) : NavKey

/** The learner's streak, answers of the last weeks and most missed words. */
@Serializable
data object HistoryNavKey : NavKey
