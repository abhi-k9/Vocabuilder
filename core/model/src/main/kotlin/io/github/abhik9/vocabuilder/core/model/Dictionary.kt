package io.github.abhik9.vocabuilder.core.model

/**
 * The meanings of a synonym or antonym that isn't a word of the packs, so it can be looked up too.
 *
 * @property senses the senses closest to the words listing it first. Never empty.
 * @property source where the meanings come from, to credit it.
 */
data class DictionaryEntry(
    val word: String,
    val senses: List<DictionarySense>,
    val source: DictionarySource,
)

/** One meaning from the dictionary, with an example of its use when the dictionary has one. */
data class DictionarySense(
    override val partOfSpeech: PartOfSpeech,
    override val definition: String,
    override val example: String? = null,
) : Meaning

enum class DictionarySource {
    /** Open English WordNet, which must be credited. */
    WORDNET,

    /** Written for Vocabuilder, as the packs are. */
    VOCABUILDER,
}
