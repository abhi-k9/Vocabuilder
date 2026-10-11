package io.github.abhik9.vocabuilder.core.domain.diagnostics

import io.github.abhik9.vocabuilder.core.testing.diagnostics.FakeDiagnosticsRepository
import io.github.abhik9.vocabuilder.core.testing.diagnostics.FakeDocumentStore
import io.github.abhik9.vocabuilder.core.testing.diagnostics.RecordingEventLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ExportDiagnosticsUseCaseTest {
    private val diagnostics = FakeDiagnosticsRepository(report = "the report")
    private val documents = FakeDocumentStore()
    private val log = RecordingEventLog()
    private val export = ExportDiagnosticsUseCase(diagnostics, documents, log)

    @Test
    fun writesTheReportToTheDocument() = runTest {
        assertTrue(export("content://diagnostics.txt"))
        assertEquals("the report", documents.documents["content://diagnostics.txt"])
    }

    @Test
    fun aFailureIsRecordedAndReported() = runTest {
        documents.failing = true

        assertFalse(export("content://diagnostics.txt"))
        assertEquals(1, log.messages.size)
        assertTrue(log.messages.single().startsWith("diagnostics: export failed"))
    }
}
