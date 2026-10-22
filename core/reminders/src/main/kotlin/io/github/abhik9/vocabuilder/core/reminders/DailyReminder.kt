package io.github.abhik9.vocabuilder.core.reminders

import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.domain.reminder.ReminderScheduler
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.domain.usecase.ObserveTodayUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** What the reminder does when its alarm goes off, or when the system cleared or moved the alarm. */
class DailyReminder @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val observeToday: ObserveTodayUseCase,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
    private val log: EventLog,
) {
    /**
     * Shows the reminder if [remind] and today's review has cards left, so it's skipped on days already studied, then
     * sets it for the next day. Does nothing once the reminder is turned off.
     */
    suspend fun run(remind: Boolean) {
        val settings = settingsRepository.settings.first()
        if (!settings.reminderEnabled) return
        if (remind) {
            val cards = observeToday().first().dailyReviewCount
            when {
                cards == 0 -> log.record { "reminder: nothing to study today" }
                notifier.show(cards) -> log.record { "reminder: shown, $cards cards" }
                else -> log.record { "reminder: notifications aren't allowed" }
            }
        }
        scheduler.schedule(settings.reminderTime)
    }
}
