package io.github.abhik9.vocabuilder.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Keeps the database's copy of the bundled word packs up to date. */
interface ContentRepository {
    /** Whether the bundled content has been checked, and copied if it changed, since the app started. */
    val isReady: StateFlow<Boolean>

    /** Copies any bundled pack that changed into the database. Learners' progress is kept. */
    suspend fun syncBundledContent()
}
