package io.github.abhik9.vocabuilder.core.data

import io.github.abhik9.vocabuilder.core.content.ContentLoader
import io.github.abhik9.vocabuilder.core.data.repository.BundledDictionaryRepository
import io.github.abhik9.vocabuilder.core.model.DictionarySource
import io.github.abhik9.vocabuilder.core.testing.diagnostics.RecordingEventLog
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BundledDictionaryRepositoryTest {
    private val log = RecordingEventLog()
    private val reads = mutableListOf<String>()
    private val files = mutableMapOf(
        "dictionary/written.json" to """
            {
              "entries": [
                {
                  "word": "toady to",
                  "senses": [{ "pos": "verb", "definition": "to flatter", "example": "He'd toady to anyone." }]
                }
              ]
            }
        """.trimIndent(),
        "dictionary/wordnet.json" to """
            { "entries": [{ "word": "subside", "senses": [{ "pos": "verb", "definition": "wear off or die down" }] }] }
        """.trimIndent(),
    )

    private fun repository(testScheduler: TestCoroutineScheduler) = BundledDictionaryRepository(
        loader = ContentLoader { path -> files[path].also { reads += path } },
        ioDispatcher = StandardTestDispatcher(testScheduler),
        log = log,
    )

    @Test
    fun findsWordsWhateverTheirCaseAndSaysWhereTheyComeFrom() = runTest {
        val repository = repository(testScheduler)

        assertEquals(DictionarySource.WORDNET, repository.getEntry("Subside")?.source)
        assertEquals(DictionarySource.VOCABUILDER, repository.getEntry("toady to")?.source)
        assertNull(repository.getEntry("abate"))
    }

    @Test
    fun readsTheFilesOnlyOnce() = runTest {
        val repository = repository(testScheduler)

        repository.preload()
        repository.getEntry("subside")
        repository.getEntry("toady to")

        assertEquals(listOf("dictionary/written.json", "dictionary/wordnet.json"), reads)
        assertTrue(log.messages.single().startsWith("dictionary: read 2 words in "), log.messages.toString())
    }

    @Test
    fun hasNoMeaningsWhenTheFilesCantBeRead() = runTest {
        files["dictionary/wordnet.json"] = "{ \"entries\": ["
        val repository = repository(testScheduler)

        assertNull(repository.getEntry("subside"))
        assertTrue(log.messages.single().startsWith("dictionary: couldn't be read"), log.messages.toString())
    }
}
