package io.github.abhik9.vocabuilder.core.testing

import io.github.abhik9.vocabuilder.core.domain.speech.Pronouncer
import kotlinx.coroutines.flow.MutableStateFlow

/** Records what it was asked to say. */
class FakePronouncer(available: Boolean = true) : Pronouncer {
    override val isAvailable = MutableStateFlow(available)

    val spoken = mutableListOf<String>()

    override fun speak(text: String) {
        spoken += text
    }
}
