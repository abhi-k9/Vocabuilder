package io.github.abhik9.vocabuilder.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.vocabuilder.core.common.diagnostics.EventLog
import io.github.abhik9.vocabuilder.core.domain.reminder.ReminderScheduler
import io.github.abhik9.vocabuilder.core.domain.reminder.nextReminderAt
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets the reminder as an alarm. It's inexact, which needs no permission: while the phone sleeps, it may come a few
 * minutes late.
 */
@Singleton
internal class AlarmReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
    private val log: EventLog,
) : ReminderScheduler {
    private val alarmManager: AlarmManager get() = context.getSystemService(AlarmManager::class.java)

    override fun schedule(time: LocalTime) {
        // The zone as it is now, as the phone may have moved to another since the app started.
        val at = nextReminderAt(ZonedDateTime.ofInstant(clock.instant(), ZoneId.systemDefault()), time)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), alarm())
        log.record { "reminder: set for $at" }
    }

    override fun cancel() {
        alarmManager.cancel(alarm())
    }

    private fun alarm(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
