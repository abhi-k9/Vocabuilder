package io.github.abhik9.vocabuilder.core.model

/** A set of words studied together, such as the GRE's common words, part 1. */
data class Deck(
    val id: String,
    val packId: String,
    val level: Level,
    /** The deck's number within its level, from 1. */
    val number: Int,
    /** The deck's place among all decks of its pack. */
    val position: Int,
)

/** How hard a deck's words are. Decks of a pack are ordered by level, then number. */
enum class Level {
    COMMON,
    INTERMEDIATE,
    ADVANCED,
}

/** A collection of decks for one exam, such as the GRE. */
data class Pack(
    val id: String,
    val name: String,
)
