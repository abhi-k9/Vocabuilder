package io.github.abhik9.vocabuilder.core.testing.diagnostics

import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.domain.repository.DiagnosticsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.DocumentStore
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow

/** Keeps every recorded message, to check what code under test records. */
class RecordingEventLog : EventLog {
    val messages = mutableListOf<String>()

    override fun record(message: () -> String) {
        messages += message()
    }
}

class FakeDiagnosticsRepository(private val report: String = "diagnostics report") : DiagnosticsRepository {
    override val isRecording = MutableStateFlow(false)
    override val logSize = MutableStateFlow(0L)

    override fun setRecording(enabled: Boolean) {
        isRecording.value = enabled
    }

    override suspend fun clear() {
        logSize.value = 0
    }

    override suspend fun report(): String = report
}

/** Documents in memory, by URI; [failing] makes writes fail as a revoked document would. */
class FakeDocumentStore(var failing: Boolean = false) : DocumentStore {
    val documents = mutableMapOf<String, String>()

    override suspend fun writeText(uri: String, text: String) {
        if (failing) throw IOException("Can't write $uri")
        documents[uri] = text
    }
}
