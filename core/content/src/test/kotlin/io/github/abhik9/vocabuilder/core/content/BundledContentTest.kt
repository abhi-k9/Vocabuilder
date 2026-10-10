package io.github.abhik9.vocabuilder.core.content

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/** Checks the content shipped with the app, read from this module's assets. */
class BundledContentTest {
    private val root = File("src/main/assets/content")
    private val loader = ContentLoader { path -> root.resolve(path).takeIf { it.isFile }?.readText() }

    @Test
    fun everyPackFollowsTheContentRules() {
        val packs = loader.loadAll()

        assertTrue(packs.isNotEmpty(), "No packs in ${root.absolutePath}")
        val problems = packs.flatMap(ContentValidator::validate)
        assertEquals(emptyList(), problems, "Content problems:\n" + problems.joinToString("\n"))
    }

    @Test
    fun everyDeckFileIsListedInItsPack() {
        loader.loadAll().forEach { pack ->
            val listed = pack.decks.map { it.id.substringAfter('/') + ".json" }.toSet()
            val present = root.resolve(pack.pack.id).list().orEmpty().filter { it != "pack.json" }.toSet()
            assertEquals(present, listed, "Deck files of ${pack.pack.id} not listed in its pack.json")
        }
    }
}
