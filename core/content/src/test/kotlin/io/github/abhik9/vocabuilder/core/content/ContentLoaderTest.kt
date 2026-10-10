package io.github.abhik9.vocabuilder.core.content

import io.github.abhik9.vocabuilder.core.model.Level
import io.github.abhik9.vocabuilder.core.model.PartOfSpeech
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test

class ContentLoaderTest {
    private val files = mutableMapOf(
        "index.json" to """{ "packs": ["gre"] }""",
        "gre/pack.json" to """{ "id": "gre", "name": "GRE", "decks": ["common-01", "advanced-01"] }""",
        "gre/common-01.json" to """
            {
              "level": "common",
              "number": 1,
              "words": [
                {
                  "word": "abate",
                  "senses": [
                    { "pos": "verb", "definition": "to become less intense", "example": "The storm abated." }
                  ],
                  "synonyms": ["subside"],
                  "antonyms": ["intensify"]
                },
                {
                  "word": "bête noire",
                  "senses": [
                    { "pos": "noun", "definition": "a thing one especially dislikes", "example": "Rain was his bête noire." }
                  ],
                  "note": "From French."
                }
              ]
            }
        """.trimIndent(),
        "gre/advanced-01.json" to """
            {
              "level": "advanced",
              "number": 1,
              "words": [
                {
                  "word": "obviate",
                  "id": "gre:obviate-old",
                  "senses": [
                    { "pos": "verb", "definition": "to remove a need", "example": "The map obviated asking." }
                  ]
                }
              ]
            }
        """.trimIndent(),
    )
    private val loader = ContentLoader { files[it] }

    @Test
    fun loadsPacksDecksAndWordsInOrder() {
        val pack = loader.loadAll().single()

        assertEquals("GRE", pack.pack.name)
        assertEquals(listOf("gre/common-01", "gre/advanced-01"), pack.decks.map { it.id })
        assertEquals(listOf(Level.COMMON, Level.ADVANCED), pack.decks.map { it.level })
        assertEquals(listOf(0, 1), pack.decks.map { it.position })
        assertEquals(listOf("gre:abate", "gre:bete-noire", "gre:obviate-old"), pack.words.map { it.id })
        assertEquals(listOf(0, 1, 0), pack.words.map { it.position })

        val abate = pack.words.first()
        assertEquals("gre/common-01", abate.deckId)
        assertEquals(PartOfSpeech.VERB, abate.primarySense.partOfSpeech)
        assertEquals(listOf("subside"), abate.synonyms)
        assertEquals(listOf("intensify"), abate.antonyms)
        assertEquals("From French.", pack.words[1].note)
    }

    @Test
    fun fingerprintChangesWithAnyFile() {
        val before = loader.load("gre").fingerprint
        assertEquals(before, loader.load("gre").fingerprint)

        files["gre/advanced-01.json"] = files.getValue("gre/advanced-01.json").replace("asking", "directions")

        assertNotEquals(before, loader.load("gre").fingerprint)
    }

    @Test
    fun rejectsUnknownKeys() {
        files["gre/advanced-01.json"] = files.getValue("gre/advanced-01.json").replace("\"senses\"", "\"sense\"")

        val error = assertFailsWith<ContentException> { loader.load("gre") }
        assertTrue("gre/advanced-01.json" in error.message.orEmpty())
    }

    @Test
    fun reportsMissingFiles() {
        files.remove("gre/common-01.json")

        val error = assertFailsWith<ContentException> { loader.load("gre") }
        assertTrue("gre/common-01.json" in error.message.orEmpty())
    }

    @Test
    fun wordIdsDropAccentsAndPunctuation() {
        assertEquals("gre:bete-noire", ContentLoader.wordId("gre", "bête noire"))
        assertEquals("gre:laissez-faire", ContentLoader.wordId("gre", "laissez-faire"))
        assertEquals("sat:abate", ContentLoader.wordId("sat", "Abate"))
    }
}
