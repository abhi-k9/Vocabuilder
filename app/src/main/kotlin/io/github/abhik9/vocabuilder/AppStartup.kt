package io.github.abhik9.vocabuilder

import io.github.abhik9.vocabuilder.core.common.di.ApplicationScope
import io.github.abhik9.vocabuilder.core.domain.repository.ContentRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Work started with the process. */
class AppStartup @Inject constructor(
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    private val contentRepository: ContentRepository,
) {
    fun start() {
        // Copy new or corrected bundled words into the database; the splash screen waits for it.
        applicationScope.launch { contentRepository.syncBundledContent() }
    }
}
