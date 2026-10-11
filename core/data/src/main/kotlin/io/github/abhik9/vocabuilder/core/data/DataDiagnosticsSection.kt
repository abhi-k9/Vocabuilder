package io.github.abhik9.vocabuilder.core.data

import io.github.abhik9.vocabuilder.core.common.di.AppDispatcher
import io.github.abhik9.vocabuilder.core.common.di.Dispatcher
import io.github.abhik9.vocabuilder.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.vocabuilder.core.content.ContentLoader
import io.github.abhik9.vocabuilder.core.database.dao.ContentDao
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.WordRepository
import io.github.abhik9.vocabuilder.core.model.MasteryState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** The words in the database, how far the learner got with them, and the settings: counts only. */
internal class DataDiagnosticsSection @Inject constructor(
    private val loader: ContentLoader,
    private val contentDao: ContentDao,
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DiagnosticsSection {
    override val title = "Data"

    // Reading and hashing the bundled packs takes a moment, so not on the main thread.
    override suspend fun describe(): List<String> = withContext(ioDispatcher) { lines() }

    private suspend fun lines(): List<String> = buildList {
        for (packId in loader.packIds()) {
            val bundled = loader.fingerprint(packId).take(FINGERPRINT_LENGTH)
            val stored = contentDao.fingerprint(packId)?.take(FINGERPRINT_LENGTH)
            add("Pack $packId: bundled $bundled, ${if (stored == bundled) "copied" else "in the database: $stored"}")
        }
        val words = wordRepository.observeWordSummaries().first()
        val states = words.groupingBy { it.progress.masteryState }.eachCount()
        val today = LocalDate.now(clock)
        add("Words: ${words.size} in ${wordRepository.observeDecks().first().size} decks")
        add("Progress: " + MasteryState.entries.joinToString { "${states[it] ?: 0} ${it.name.lowercase()}" })
        add(
            "Due today: ${words.count { it.progress.isDue(today) }}, " +
                "starred: ${words.count { it.progress.starred }}, " +
                "marked known: ${words.count { it.progress.alreadyKnown }}",
        )
        val startOfToday = today.atStartOfDay(clock.zone).toInstant()
        val reviewsToday = progressRepository.observeReviewCountSince(startOfToday).first()
        add("Reviews: ${progressRepository.observeReviewCountSince(Instant.EPOCH).first()}, today: $reviewsToday")
        add("Settings: ${settingsRepository.settings.first()}")
    }

    private companion object {
        const val FINGERPRINT_LENGTH = 12
    }
}
