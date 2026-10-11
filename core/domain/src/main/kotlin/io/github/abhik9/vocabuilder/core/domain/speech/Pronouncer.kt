package io.github.abhik9.vocabuilder.core.domain.speech

import kotlinx.coroutines.flow.StateFlow

/** Says words aloud with the device's text-to-speech. */
interface Pronouncer {
    /** Whether an English voice is ready; pronunciation buttons are hidden otherwise. */
    val isAvailable: StateFlow<Boolean>

    /** Says [text], interrupting anything still being said. */
    fun speak(text: String)
}
