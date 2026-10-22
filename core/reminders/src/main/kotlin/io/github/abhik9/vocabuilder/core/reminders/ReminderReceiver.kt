package io.github.abhik9.vocabuilder.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.vocabuilder.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Receives the reminder's alarm, and the system's restarts and changes of clock, after which the alarm is set again.
 * Its dependencies come from an entry point: Hilt can't inject a receiver written in Kotlin that overrides
 * onReceive().
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dependencies = EntryPointAccessors.fromApplication(context, Dependencies::class.java)
        val pending = goAsync()
        dependencies.applicationScope().launch {
            try {
                dependencies.dailyReminder().run(remind = intent.action == ACTION_REMIND)
            } finally {
                pending.finish()
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        @ApplicationScope
        fun applicationScope(): CoroutineScope

        fun dailyReminder(): DailyReminder
    }

    companion object {
        const val ACTION_REMIND = "io.github.abhik9.vocabuilder.action.REMIND"
    }
}
