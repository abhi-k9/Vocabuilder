package io.github.abhik9.vocabuilder

import io.github.abhik9.vocabuilder.core.common.AppInfo
import io.github.abhik9.vocabuilder.core.common.di.ApplicationScope
import io.github.abhik9.vocabuilder.core.diagnostics.DiagnosticsLog
import io.github.abhik9.vocabuilder.core.domain.reminder.ReminderScheduler
import io.github.abhik9.vocabuilder.core.domain.repository.ContentRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Work started with the process. */
class AppStartup @Inject constructor(
    @param:ApplicationScope private val applicationScope: CoroutineScope,
    private val contentRepository: ContentRepository,
    private val settingsRepository: UserSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val diagnosticsLog: DiagnosticsLog,
    private val appInfo: AppInfo,
) {
    fun start() {
        diagnosticsLog.recordCrashes()
        diagnosticsLog.record { "app: process started, version ${appInfo.versionName}" }

        // Copy new or corrected bundled words into the database; the splash screen waits for it.
        applicationScope.launch { contentRepository.syncBundledContent() }

        // Keep the daily reminder set as the settings say: from the start, as an alarm doesn't survive the app being
        // force stopped, and whenever they change.
        applicationScope.launch {
            settingsRepository.settings
                .map { if (it.reminderEnabled) it.reminderTime else null }
                .distinctUntilChanged()
                .collect { time -> if (time != null) reminderScheduler.schedule(time) else reminderScheduler.cancel() }
        }
    }
}
