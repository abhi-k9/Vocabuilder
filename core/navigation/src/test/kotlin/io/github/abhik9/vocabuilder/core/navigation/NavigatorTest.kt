package io.github.abhik9.vocabuilder.core.navigation

import androidx.navigation3.runtime.NavKey
import io.github.abhik9.vocabuilder.core.model.StudyMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class NavigatorTest {
    private val backStack = mutableListOf<NavKey>()
    private val navigator = Navigator(backStack)

    @Test
    fun startsAtTheRoot() {
        assertEquals(listOf(Navigator.ROOT), backStack)
        assertFalse(navigator.goBack())
    }

    @Test
    fun detailScreensStackUpAndGoBack() {
        navigator.navigate(DeckNavKey("gre/common-01"))
        navigator.navigate(StudyNavKey(StudyMode.DECK, "gre/common-01"))

        assertEquals(StudyNavKey(StudyMode.DECK, "gre/common-01"), navigator.currentKey)
        assertEquals(HomeNavKey, navigator.currentTopLevelKey)
        assertTrue(navigator.goBack())
        assertEquals(DeckNavKey("gre/common-01"), navigator.currentKey)
    }

    @Test
    fun replaceSwapsTheCurrentScreen() {
        navigator.navigate(StudyNavKey(StudyMode.DECK, "gre/common-01"))

        navigator.replace(StudyNavKey(StudyMode.DECK, "gre/common-02"))

        assertEquals(listOf(Navigator.ROOT, StudyNavKey(StudyMode.DECK, "gre/common-02")), backStack)
    }

    @Test
    fun topLevelScreensSitOnTheRoot() {
        navigator.navigate(DeckNavKey("gre/common-01"))

        navigator.navigate(DecksNavKey)
        assertEquals(listOf<NavKey>(HomeNavKey, DecksNavKey), backStack)
        assertEquals(DecksNavKey, navigator.currentTopLevelKey)

        navigator.navigate(DeckNavKey("gre/common-01"))
        navigator.navigate(HomeNavKey)
        assertEquals(listOf<NavKey>(HomeNavKey), backStack)
    }
}
