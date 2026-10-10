package io.github.abhik9.vocabuilder.core.testing

import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.Pack
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import io.github.abhik9.vocabuilder.core.model.Sense
import io.github.abhik9.vocabuilder.core.model.Word
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A small pack of two decks for tests. */
object TestData {
    val TODAY: LocalDate = LocalDate.of(2026, 10, 10)

    /** Noon on [TODAY], in UTC. */
    val NOW: Instant = TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC)

    val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

    val pack = Pack(id = "gre", name = "GRE")

    val commonDeck = Deck(id = "gre/common-01", packId = "gre", level = Level.COMMON, number = 1, position = 0)

    val advancedDeck = Deck(id = "gre/advanced-01", packId = "gre", level = Level.ADVANCED, number = 1, position = 1)

    val decks = listOf(commonDeck, advancedDeck)

    fun word(headword: String, deck: Deck = commonDeck, position: Int = 0) = Word(
        id = "gre:$headword",
        deckId = deck.id,
        headword = headword,
        senses = listOf(
            Sense(PartOfSpeech.VERB, "the meaning of $headword", "An example with $headword in it."),
        ),
        synonyms = listOf("synonym"),
        position = position,
    )

    val commonWords = listOf("abate", "laud", "wane", "zeal").mapIndexed { index, it -> word(it, commonDeck, index) }

    val advancedWords = listOf("pique", "vex").mapIndexed { index, it -> word(it, advancedDeck, index) }

    val words = commonWords + advancedWords
}
