package io.github.abhik9.vocabuilder.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * The diagnostics log: what the app did, recorded on the device only while recording is on, and
 * only shared when the user exports it.
 */
interface DiagnosticsRepository {
    val isRecording: StateFlow<Boolean>

    /** Size of the recorded log, in bytes. */
    val logSize: StateFlow<Long>

    fun setRecording(enabled: Boolean)

    suspend fun clear()

    /** The state of the app and device, followed by the recorded log, oldest entries first. */
    suspend fun report(): String
}

/** Documents the user picks with the system's file picker, by URI. */
interface DocumentStore {
    /** Replaces the document's content. Throws an IOException when it can't be written. */
    suspend fun writeText(uri: String, text: String)
}
