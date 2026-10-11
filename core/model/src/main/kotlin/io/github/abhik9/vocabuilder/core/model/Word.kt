package io.github.abhik9.vocabuilder.core.model

/**
 * A word to learn.
 *
 * @property id stable across content updates, such as `gre:abate`: progress is keyed by it.
 * @property headword the word as shown on the card.
 * @property senses its meanings, most common first. Never empty.
 * @property note how the word is used: its tone, a common confusion, or how to remember it.
 * @property position the word's place in its deck.
 */
data class Word(
    val id: String,
    val deckId: String,
    val headword: String,
    val senses: List<Sense>,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val note: String? = null,
    val position: Int = 0,
) {
    val primarySense: Sense get() = senses.first()
}

/** A meaning as shown: a word's own sense, or one from the dictionary. */
interface Meaning {
    val partOfSpeech: PartOfSpeech
    val definition: String

    /** Shows the word in use, when there's an example. */
    val example: String?
}

/** One meaning of a word, with a sentence showing it in use. */
data class Sense(
    override val partOfSpeech: PartOfSpeech,
    override val definition: String,
    override val example: String,
) : Meaning

enum class PartOfSpeech {
    NOUN,
    VERB,
    ADJECTIVE,
    ADVERB,
}
