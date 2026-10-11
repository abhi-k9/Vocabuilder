package io.github.abhik9.vocabuilder.core.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.vocabuilder.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.domain.speech.Pronouncer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pronounces words with the system's text-to-speech engine, in American English. The engine is
 * connected when first needed and kept for the life of the process; it works offline with the
 * voices installed on the device.
 */
@Singleton
internal class TextToSpeechPronouncer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val log: EventLog,
) : Pronouncer {
    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private var engine: TextToSpeech? = null
    private var isReady = false

    /** Asked for before the engine was ready; said once it is. */
    private var pendingText: String? = null

    /** How connecting to the engine went, for diagnostics. */
    var state = "connecting"
        private set

    init {
        // Called on the main thread once the engine is connected, or failed to.
        engine = TextToSpeech(context) { status ->
            val tts = engine
            val language = if (tts != null && status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US) else null
            isReady = language != null && language >= TextToSpeech.LANG_AVAILABLE
            state = "engine ${tts?.defaultEngine}, " +
                (if (language == null) "connection failed ($status)" else "US English: ${languageState(language)}")
            log.record { "speech: $state" }
            _isAvailable.value = isReady
            pendingText?.takeIf { isReady }?.let(::speak)
            pendingText = null
        }
    }

    override fun speak(text: String) {
        val tts = engine
        if (tts == null || !isReady) {
            pendingText = text
            return
        }
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        if (result != TextToSpeech.SUCCESS) log.record { "speech: speaking failed ($result)" }
    }

    private fun languageState(result: Int) = when (result) {
        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE, TextToSpeech.LANG_COUNTRY_AVAILABLE -> "available"
        TextToSpeech.LANG_AVAILABLE -> "available for another country"
        TextToSpeech.LANG_MISSING_DATA -> "voice data missing"
        TextToSpeech.LANG_NOT_SUPPORTED -> "not supported"
        else -> "unknown ($result)"
    }

    private companion object {
        const val UTTERANCE_ID = "word"
    }
}

/** Whether words can be said aloud, and how connecting to the text-to-speech engine went. */
internal class SpeechDiagnosticsSection @Inject constructor(
    private val pronouncer: TextToSpeechPronouncer,
) : DiagnosticsSection {
    override val title = "Speech"

    override suspend fun describe() = listOf(
        "Available: ${pronouncer.isAvailable.value}",
        "Text-to-speech: ${pronouncer.state}",
    )
}
