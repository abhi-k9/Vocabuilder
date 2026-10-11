package io.github.abhik9.vocabuilder.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Moves around a single back stack whose bottom entry is always the root screen. The other
 * top-level screens sit directly on top of it, so Back from them returns to the root, as users
 * expect.
 */
class Navigator(private val backStack: MutableList<NavKey>) {
    init {
        if (backStack.isEmpty()) backStack.add(ROOT)
    }

    val currentKey: NavKey get() = backStack.last()

    /** The top-level screen the user is in: the last top-level key on the stack. */
    val currentTopLevelKey: NavKey get() = backStack.last { isTopLevel(it) }

    fun navigate(key: NavKey) {
        if (isTopLevel(key)) {
            resetTo(*listOfNotNull(ROOT, key.takeIf { it != ROOT }).toTypedArray())
        } else {
            backStack.add(key)
        }
    }

    /** Replaces the current screen, so Back skips it: used to go from one session to the next. */
    fun replace(key: NavKey) {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(key)
    }

    /** Returns false when there is nothing to go back to, so the caller can finish the app. */
    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    private fun resetTo(vararg keys: NavKey) {
        backStack.clear()
        backStack.addAll(keys)
    }

    companion object {
        /** The screen at the bottom of the back stack, where the app starts. */
        val ROOT: NavKey = HomeNavKey

        private val TOP_LEVEL: Set<NavKey> = setOf(HomeNavKey, DecksNavKey, WordsNavKey, SettingsNavKey)

        fun isTopLevel(key: NavKey): Boolean = key in TOP_LEVEL
    }
}
