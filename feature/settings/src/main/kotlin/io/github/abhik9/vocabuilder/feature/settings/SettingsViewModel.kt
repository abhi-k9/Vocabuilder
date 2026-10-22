package io.github.abhik9.vocabuilder.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.vocabuilder.core.common.AppInfo
import io.github.abhik9.vocabuilder.core.common.WhileUiSubscribed
import io.github.abhik9.vocabuilder.core.domain.diagnostics.ExportDiagnosticsUseCase
import io.github.abhik9.vocabuilder.core.domain.repository.DiagnosticsRepository
import io.github.abhik9.vocabuilder.core.domain.repository.ProgressRepository
import io.github.abhik9.vocabuilder.core.domain.repository.UserSettingsRepository
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object ProgressReset : SettingsEvent

    data class DiagnosticsExported(val success: Boolean) : SettingsEvent
}

/** @property logBytes the size of the recorded diagnostics log. */
data class DiagnosticsState(
    val isRecording: Boolean,
    val logBytes: Long,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val progressRepository: ProgressRepository,
    private val diagnosticsRepository: DiagnosticsRepository,
    private val exportDiagnostics: ExportDiagnosticsUseCase,
    private val clock: Clock,
    val appInfo: AppInfo,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, WhileUiSubscribed, null)

    val diagnostics: StateFlow<DiagnosticsState> =
        combine(diagnosticsRepository.isRecording, diagnosticsRepository.logSize, ::DiagnosticsState).stateIn(
            viewModelScope,
            WhileUiSubscribed,
            DiagnosticsState(diagnosticsRepository.isRecording.value, diagnosticsRepository.logSize.value),
        )

    /** A dated default name for the diagnostics file, e.g. "vocabuilder-diagnostics-2026-10-10.txt". */
    val suggestedDiagnosticsFileName: String get() = "vocabuilder-diagnostics-${LocalDate.now(clock)}.txt"

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    fun setThemeMode(themeMode: ThemeMode) = launch { settingsRepository.setThemeMode(themeMode) }

    fun setUseDynamicColor(useDynamicColor: Boolean) = launch { settingsRepository.setUseDynamicColor(useDynamicColor) }

    fun setNewWordsPerDay(count: Int) = launch { settingsRepository.setNewWordsPerDay(count) }

    fun setCardFront(cardFront: CardFront) = launch { settingsRepository.setCardFront(cardFront) }

    fun setAutoPronounce(autoPronounce: Boolean) = launch { settingsRepository.setAutoPronounce(autoPronounce) }

    fun setShuffleCards(shuffle: Boolean) = launch { settingsRepository.setShuffleCards(shuffle) }

    fun setReminderEnabled(enabled: Boolean) = launch { settingsRepository.setReminderEnabled(enabled) }

    fun setReminderTime(time: LocalTime) = launch { settingsRepository.setReminderTime(time) }

    fun resetAllProgress() = launch {
        progressRepository.resetAll()
        _events.send(SettingsEvent.ProgressReset)
    }

    fun setDiagnosticsRecording(enabled: Boolean) = diagnosticsRepository.setRecording(enabled)

    fun clearDiagnostics() = launch { diagnosticsRepository.clear() }

    fun exportDiagnosticsTo(uri: String) = launch {
        _events.send(SettingsEvent.DiagnosticsExported(success = exportDiagnostics(uri)))
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
