package io.github.abhik9.vocabuilder.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.abhik9.vocabuilder.core.content.ContentLoader
import io.github.abhik9.vocabuilder.core.data.repository.BundledContentRepository
import io.github.abhik9.vocabuilder.core.data.repository.RoomProgressRepository
import io.github.abhik9.vocabuilder.core.data.repository.RoomWordRepository
import io.github.abhik9.vocabuilder.core.database.VocabuilderDatabase
import io.github.abhik9.vocabuilder.core.model.WordProgress
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundledContentRepositoryTest {
    private lateinit var database: VocabuilderDatabase

    private val files = mutableMapOf(
        "index.json" to """{ "packs": ["gre"] }""",
        "gre/pack.json" to """{ "id": "gre", "name": "GRE", "decks": ["common-01"] }""",
        "gre/common-01.json" to deck("abate", "wane"),
    )

    private fun deck(vararg words: String) = """
        {
          "level": "common",
          "number": 1,
          "words": [${words.joinToString(",") { word(it) }}]
        }
    """.trimIndent()

    private fun word(headword: String) = """
        {
          "word": "$headword",
          "senses": [{ "pos": "verb", "definition": "to $headword", "example": "It will $headword." }]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            VocabuilderDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun copiesTheBundledPacksAndBecomesReady() = runTest {
        val content = repository(StandardTestDispatcher(testScheduler))
        assertFalse(content.isReady.value)

        content.syncBundledContent()

        assertTrue(content.isReady.value)
        val words = RoomWordRepository(database.contentDao())
        assertEquals(listOf("abate", "wane"), words.observeWordSummaries().first().map { it.headword }.sorted())
        assertEquals(listOf("gre/common-01"), words.observeDecks().first().map { it.id })
    }

    @Test
    fun updatesChangedPacksAndKeepsProgress() = runTest {
        val content = repository(StandardTestDispatcher(testScheduler))
        content.syncBundledContent()
        RoomProgressRepository(database.progressDao()).setStarred("gre:abate", true)

        files["gre/common-01.json"] = deck("abate", "laud")
        content.syncBundledContent()

        val words = RoomWordRepository(database.contentDao()).observeWordSummaries().first().associateBy { it.headword }
        assertEquals(setOf("abate", "laud"), words.keys)
        assertEquals(WordProgress("gre:abate", starred = true), words.getValue("abate").progress)
    }

    @Test
    fun leavesUnchangedPacksAlone() = runTest {
        val content = repository(StandardTestDispatcher(testScheduler))
        content.syncBundledContent()
        val dao = database.contentDao()
        dao.deleteSenses("gre")

        content.syncBundledContent()

        // The fingerprint still matches, so the (now broken) copy wasn't refreshed.
        assertEquals(emptyList(), dao.observeWordSummaries().first())
    }

    @Test
    fun becomesReadyEvenIfAPackIsBroken() = runTest {
        files["gre/common-01.json"] = "{ not json"
        val content = repository(StandardTestDispatcher(testScheduler))

        runCatching { content.syncBundledContent() }

        assertTrue(content.isReady.value)
    }

    private fun repository(dispatcher: CoroutineDispatcher) = BundledContentRepository(
        loader = ContentLoader { files[it] },
        contentDao = database.contentDao(),
        ioDispatcher = dispatcher,
    )
}
