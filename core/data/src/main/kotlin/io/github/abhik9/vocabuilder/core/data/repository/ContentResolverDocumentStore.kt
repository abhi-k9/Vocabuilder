package io.github.abhik9.vocabuilder.core.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.vocabuilder.core.common.di.AppDispatcher
import io.github.abhik9.vocabuilder.core.common.di.Dispatcher
import io.github.abhik9.vocabuilder.core.domain.repository.DocumentStore
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Writes documents chosen through the Storage Access Framework. */
internal class ContentResolverDocumentStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DocumentStore {
    override suspend fun writeText(uri: String, text: String) = withContext(ioDispatcher) {
        try {
            // "wt" truncates, so overwriting an existing, longer file leaves no stale tail.
            val stream = context.contentResolver.openOutputStream(Uri.parse(uri), "wt")
                ?: throw IOException("Can't open $uri for writing")
            stream.bufferedWriter().use { it.write(text) }
        } catch (e: IOException) {
            throw e
        } catch (e: RuntimeException) {
            // Document providers report failures in different ways, for example a revoked
            // permission as a SecurityException: all of them become the IOException callers expect.
            throw IOException("Can't write $uri", e)
        }
    }
}
