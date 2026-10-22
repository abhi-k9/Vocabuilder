package io.github.abhik9.vocabuilder.core.reminders.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.vocabuilder.core.domain.reminder.ReminderScheduler
import io.github.abhik9.vocabuilder.core.reminders.AlarmReminderScheduler

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RemindersModule {
    @Binds
    abstract fun bindsReminderScheduler(scheduler: AlarmReminderScheduler): ReminderScheduler
}
