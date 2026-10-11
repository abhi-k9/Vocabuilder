package io.github.abhik9.vocabuilder.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The JSON format of the content files. It is kept apart from the domain models, so the models
// can change without touching the files. Unknown keys are rejected, to catch typos.

/** `index.json`: the packs to load, in order. */
@Serializable
internal data class IndexFile(val packs: List<String>)

/** `<pack>/pack.json`: the pack and its deck files, in study order. */
@Serializable
internal data class PackFile(
    val id: String,
    val name: String,
    val decks: List<String>,
)

/** `<pack>/<deck>.json`: one deck and its words, in study order. */
@Serializable
internal data class DeckFile(
    val level: LevelValue,
    val number: Int,
    val words: List<WordValue>,
)

@Serializable
internal data class WordValue(
    val word: String,
    val senses: List<SenseValue>,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val note: String? = null,
    /** Keeps a word's identity, and so its progress, if its spelling is ever corrected. */
    val id: String? = null,
)

@Serializable
internal data class SenseValue(
    val pos: PartOfSpeechValue,
    val definition: String,
    val example: String,
)

/**
 * `dictionary/wordnet.json` and `dictionary/written.json`: the meanings of the synonyms and antonyms that aren't words
 * of the packs, from Open English WordNet and written for Vocabuilder.
 */
@Serializable
internal data class DictionaryFile(val entries: List<DictionaryEntryValue>)

@Serializable
internal data class DictionaryEntryValue(
    val word: String,
    val senses: List<DictionarySenseValue>,
)

@Serializable
internal data class DictionarySenseValue(
    val pos: PartOfSpeechValue,
    val definition: String,
    val example: String? = null,
)

@Serializable
internal enum class LevelValue {
    @SerialName("common")
    COMMON,

    @SerialName("intermediate")
    INTERMEDIATE,

    @SerialName("advanced")
    ADVANCED,
}

@Serializable
internal enum class PartOfSpeechValue {
    @SerialName("noun")
    NOUN,

    @SerialName("verb")
    VERB,

    @SerialName("adjective")
    ADJECTIVE,

    @SerialName("adverb")
    ADVERB,
}
