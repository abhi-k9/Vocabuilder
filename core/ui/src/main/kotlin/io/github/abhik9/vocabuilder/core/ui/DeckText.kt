package io.github.abhik9.vocabuilder.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech

/** "Common", "Intermediate" or "Advanced". */
@Composable
@ReadOnlyComposable
fun Level.label(): String = stringResource(
    when (this) {
        Level.COMMON -> R.string.core_ui_level_common
        Level.INTERMEDIATE -> R.string.core_ui_level_intermediate
        Level.ADVANCED -> R.string.core_ui_level_advanced
    },
)

/** "Common words", as a heading for the level's decks. */
@Composable
@ReadOnlyComposable
fun Level.heading(): String = stringResource(
    when (this) {
        Level.COMMON -> R.string.core_ui_level_common_words
        Level.INTERMEDIATE -> R.string.core_ui_level_intermediate_words
        Level.ADVANCED -> R.string.core_ui_level_advanced_words
    },
)

/** "Common · Deck 3". */
@Composable
@ReadOnlyComposable
fun Deck.title(): String = stringResource(R.string.core_ui_deck_title, level.label(), number)

/** "Deck 3", under a heading naming the level. */
@Composable
@ReadOnlyComposable
fun Deck.shortTitle(): String = stringResource(R.string.core_ui_deck_number, number)

/** "noun", or "n." when [short]. */
@Composable
@ReadOnlyComposable
fun PartOfSpeech.label(short: Boolean = false): String = stringResource(
    when (this) {
        PartOfSpeech.NOUN -> if (short) R.string.core_ui_pos_noun_short else R.string.core_ui_pos_noun
        PartOfSpeech.VERB -> if (short) R.string.core_ui_pos_verb_short else R.string.core_ui_pos_verb
        PartOfSpeech.ADJECTIVE -> if (short) R.string.core_ui_pos_adjective_short else R.string.core_ui_pos_adjective
        PartOfSpeech.ADVERB -> if (short) R.string.core_ui_pos_adverb_short else R.string.core_ui_pos_adverb
    },
)
