package io.github.abhik9.vocabuilder.core.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
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
) : Pronouncer {
    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private var engine: TextToSpeech? = null
    private var isReady = false

    /** Asked for before the engine was ready; said once it is. */
    private var pendingText: String? = null

    init {
        // Called on the main thread once the engine is connected, or failed to.
        engine = TextToSpeech(context) { status ->
            val tts = engine
            isReady = tts != null && status == TextToSpeech.SUCCESS &&
                tts.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE
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
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private companion object {
        const val UTTERANCE_ID = "word"
    }
}
