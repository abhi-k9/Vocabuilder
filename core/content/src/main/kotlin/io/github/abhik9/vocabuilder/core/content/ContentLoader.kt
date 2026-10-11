package io.github.abhik9.vocabuilder.core.content

import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.DictionaryEntry
import io.github.abhik9.vocabuilder.core.model.DictionarySense
import io.github.abhik9.vocabuilder.core.model.DictionarySource
import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.Pack
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import io.github.abhik9.vocabuilder.core.model.Sense
import io.github.abhik9.vocabuilder.core.model.Word
import java.security.MessageDigest
import java.text.Normalizer
import kotlinx.serialization.json.Json

/**
 * A pack as bundled with the app.
 *
 * @property fingerprint changes whenever any of the pack's files does, so the database copy can be
 * refreshed only when needed.
 */
data class BundledPack(
    val pack: Pack,
    val decks: List<Deck>,
    val words: List<Word>,
    val fingerprint: String,
)

class ContentException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Reads the bundled word packs. */
class ContentLoader(private val files: ContentFiles) {
    private val json = Json { ignoreUnknownKeys = false }

    /** The ids of the bundled packs, in order. */
    fun packIds(): List<String> = decode<IndexFile>(INDEX).packs

    fun loadAll(): List<BundledPack> = packIds().map(::load)

    /**
     * The fingerprint [load] would give the pack, computed from its files without decoding its
     * decks, to check cheaply whether the pack changed.
     */
    fun fingerprint(packId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val packPath = "$packId/pack.json"
        val packText = readFile(packPath).also { digest.update(it.toByteArray()) }
        decodeText<PackFile>(packPath, packText).decks.forEach { deckName ->
            digest.update(readFile("$packId/$deckName.json").toByteArray())
        }
        return digest.hex()
    }

    fun load(packId: String): BundledPack {
        val digest = MessageDigest.getInstance("SHA-256")
        fun readTracked(path: String): String = readFile(path).also { digest.update(it.toByteArray()) }

        val packPath = "$packId/pack.json"
        val packFile = decodeText<PackFile>(packPath, readTracked(packPath))
        if (packFile.id != packId) throw ContentException("$packPath has id \"${packFile.id}\", expected \"$packId\"")

        val decks = mutableListOf<Deck>()
        val words = mutableListOf<Word>()
        packFile.decks.forEachIndexed { deckIndex, deckName ->
            val deckPath = "$packId/$deckName.json"
            val deckFile = decodeText<DeckFile>(deckPath, readTracked(deckPath))
            val deck = Deck(
                id = "$packId/$deckName",
                packId = packId,
                level = deckFile.level.toModel(),
                number = deckFile.number,
                position = deckIndex,
            )
            decks += deck
            deckFile.words.forEachIndexed { wordIndex, value -> words += value.toModel(packId, deck.id, wordIndex) }
        }
        return BundledPack(
            pack = Pack(id = packFile.id, name = packFile.name),
            decks = decks,
            words = words,
            fingerprint = digest.hex(),
        )
    }

    /**
     * The dictionary of the synonyms and antonyms that aren't words of the packs: the entries written for Vocabuilder,
     * then those from Open English WordNet.
     */
    fun loadDictionary(): List<DictionaryEntry> =
        decode<DictionaryFile>(WRITTEN_DICTIONARY).entries.map { it.toModel(DictionarySource.VOCABUILDER) } +
            decode<DictionaryFile>(WORDNET_DICTIONARY).entries.map { it.toModel(DictionarySource.WORDNET) }

    private fun MessageDigest.hex(): String = digest().joinToString("") { "%02x".format(it) }

    private fun readFile(path: String): String =
        files.read(path) ?: throw ContentException("Missing content file $path")

    private inline fun <reified T> decode(path: String): T = decodeText(path, readFile(path))

    private inline fun <reified T> decodeText(path: String, text: String): T = try {
        json.decodeFromString<T>(text)
    } catch (e: IllegalArgumentException) {
        throw ContentException("Invalid content file $path: ${e.message}", e)
    }

    companion object {
        const val INDEX = "index.json"
        const val WORDNET_DICTIONARY = "dictionary/wordnet.json"
        const val WRITTEN_DICTIONARY = "dictionary/written.json"

        /** The id a word gets unless its file sets one: `gre:bete-noire` for "bête noire". */
        fun wordId(packId: String, headword: String): String = "$packId:${slug(headword)}"

        private fun slug(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }
}

private fun WordValue.toModel(packId: String, deckId: String, position: Int) = Word(
    id = id ?: ContentLoader.wordId(packId, word),
    deckId = deckId,
    headword = word,
    senses = senses.map { Sense(it.pos.toModel(), it.definition, it.example) },
    synonyms = synonyms,
    antonyms = antonyms,
    note = note,
    position = position,
)

private fun DictionaryEntryValue.toModel(source: DictionarySource) = DictionaryEntry(
    word = word,
    senses = senses.map { DictionarySense(it.pos.toModel(), it.definition, it.example) },
    source = source,
)

private fun LevelValue.toModel() = when (this) {
    LevelValue.COMMON -> Level.COMMON
    LevelValue.INTERMEDIATE -> Level.INTERMEDIATE
    LevelValue.ADVANCED -> Level.ADVANCED
}

private fun PartOfSpeechValue.toModel() = when (this) {
    PartOfSpeechValue.NOUN -> PartOfSpeech.NOUN
    PartOfSpeechValue.VERB -> PartOfSpeech.VERB
    PartOfSpeechValue.ADJECTIVE -> PartOfSpeech.ADJECTIVE
    PartOfSpeechValue.ADVERB -> PartOfSpeech.ADVERB
}
