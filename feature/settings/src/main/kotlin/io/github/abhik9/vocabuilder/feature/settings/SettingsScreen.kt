package io.github.abhik9.vocabuilder.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.common.AppInfo
import io.github.abhik9.vocabuilder.core.designsystem.component.SectionHeader
import io.github.abhik9.vocabuilder.core.designsystem.theme.supportsDynamicTheming
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.model.ThemeMode
import io.github.abhik9.vocabuilder.core.model.UserSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun SettingsRoute(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources by rememberUpdatedState(LocalResources.current)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SettingsEvent.ProgressReset -> R.string.feature_settings_reset_done

                is SettingsEvent.DiagnosticsExported -> if (event.success) {
                    R.string.feature_settings_diagnostics_exported
                } else {
                    R.string.feature_settings_diagnostics_export_failed
                }
            }
            snackbarHostState.showSnackbar(resources.getString(message))
        }
    }

    val diagnosticsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(DIAGNOSTICS_MIME_TYPE),
    ) { uri -> uri?.let { viewModel.exportDiagnosticsTo(it.toString()) } }

    SettingsScreen(
        settings = settings,
        diagnostics = diagnostics,
        appInfo = viewModel.appInfo,
        snackbarHostState = snackbarHostState,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setUseDynamicColor,
        onNewWordsPerDayChange = viewModel::setNewWordsPerDay,
        onCardFrontChange = viewModel::setCardFront,
        onAutoPronounceChange = viewModel::setAutoPronounce,
        onShuffleChange = viewModel::setShuffleCards,
        onReminderChange = rememberReminderToggle(snackbarHostState, viewModel::setReminderEnabled),
        onReminderTimeChange = viewModel::setReminderTime,
        onResetAllProgress = viewModel::resetAllProgress,
        onDiagnosticsRecordingChange = viewModel::setDiagnosticsRecording,
        onExportDiagnostics = { diagnosticsExportLauncher.launch(viewModel.suggestedDiagnosticsFileName) },
        onClearDiagnostics = viewModel::clearDiagnostics,
    )
}

/**
 * Turns the reminder on or off with [setEnabled]. Turning it on first asks for Android's permission to notify, if it
 * isn't granted yet; once it's refused for good, Android no longer asks, so a snackbar offers the app's notification
 * settings instead.
 */
@Composable
private fun rememberReminderToggle(
    snackbarHostState: SnackbarHostState,
    setEnabled: (Boolean) -> Unit,
): (Boolean) -> Unit {
    val context = LocalContext.current
    val resources by rememberUpdatedState(LocalResources.current)
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            setEnabled(true)
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = resources.getString(R.string.feature_settings_reminder_not_allowed),
                    actionLabel = resources.getString(R.string.feature_settings_reminder_open_settings),
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            }
        }
    }
    return { enabled ->
        val mustAsk = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (mustAsk) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else setEnabled(enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    settings: UserSettings?,
    diagnostics: DiagnosticsState,
    appInfo: AppInfo,
    snackbarHostState: SnackbarHostState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onNewWordsPerDayChange: (Int) -> Unit,
    onCardFrontChange: (CardFront) -> Unit,
    onAutoPronounceChange: (Boolean) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onResetAllProgress: () -> Unit,
    onDiagnosticsRecordingChange: (Boolean) -> Unit,
    onExportDiagnostics: () -> Unit,
    onClearDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (settings == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            AppearanceSection(settings, onThemeModeChange, onDynamicColorChange)
            StudySection(settings, onNewWordsPerDayChange, onCardFrontChange, onAutoPronounceChange, onShuffleChange)
            ReminderSection(settings, onReminderChange, onReminderTimeChange)

            SectionHeader(stringResource(R.string.feature_settings_section_progress))
            SettingsRow(
                icon = Icons.Outlined.RestartAlt,
                title = stringResource(R.string.feature_settings_reset_all),
                subtitle = stringResource(R.string.feature_settings_reset_all_summary),
                onClick = { showResetDialog = true },
            )

            DiagnosticsSection(diagnostics, onDiagnosticsRecordingChange, onExportDiagnostics, onClearDiagnostics)
            AboutSection(appInfo)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.feature_settings_reset_all_title)) },
            text = { Text(stringResource(R.string.feature_settings_reset_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetAllProgress()
                    },
                ) { Text(stringResource(R.string.feature_settings_reset)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                ) { Text(stringResource(R.string.feature_settings_cancel)) }
            },
        )
    }
}

@Composable
private fun ReminderSection(
    settings: UserSettings,
    onReminderChange: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
) {
    var showTimeDialog by rememberSaveable { mutableStateOf(false) }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time = settings.reminderTime.format(DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a"))
    SectionHeader(stringResource(R.string.feature_settings_section_reminder))
    SwitchRow(
        icon = Icons.Outlined.NotificationsActive,
        title = stringResource(R.string.feature_settings_reminder),
        subtitle = stringResource(R.string.feature_settings_reminder_summary, time),
        checked = settings.reminderEnabled,
        onCheckedChange = onReminderChange,
    )
    if (settings.reminderEnabled) {
        SettingsRow(
            icon = Icons.Outlined.Schedule,
            title = stringResource(R.string.feature_settings_reminder_time),
            subtitle = time,
            onClick = { showTimeDialog = true },
        )
    }
    if (showTimeDialog) {
        ReminderTimeDialog(
            initial = settings.reminderTime,
            is24Hour = is24Hour,
            onDismiss = { showTimeDialog = false },
            onConfirm = { chosen ->
                showTimeDialog = false
                onReminderTimeChange(chosen)
            },
        )
    }
}

/** Picks the reminder's time, typed in: it fits a dialog on any screen, unlike the clock dial. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initial: LocalTime,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_settings_reminder_time)) },
        text = { TimeInput(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.feature_settings_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_settings_cancel)) }
        },
    )
}

/** One of [options] to pick, as a row of segmented buttons. */
@Composable
private fun <T> SegmentedChoices(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    settings: UserSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.feature_settings_section_appearance))
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_theme)) },
        leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
        supportingContent = {
            SegmentedChoices(ThemeMode.entries, settings.themeMode, onThemeModeChange) { it.label() }
        },
    )
    if (supportsDynamicTheming()) {
        SwitchRow(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.feature_settings_dynamic_color),
            subtitle = stringResource(R.string.feature_settings_dynamic_color_summary),
            checked = settings.useDynamicColor,
            onCheckedChange = onDynamicColorChange,
        )
    }
}

@Composable
private fun StudySection(
    settings: UserSettings,
    onNewWordsPerDayChange: (Int) -> Unit,
    onCardFrontChange: (CardFront) -> Unit,
    onAutoPronounceChange: (Boolean) -> Unit,
    onShuffleChange: (Boolean) -> Unit,
) {
    SectionHeader(stringResource(R.string.feature_settings_section_study))
    NewWordsRow(settings.newWordsPerDay, onNewWordsPerDayChange)
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_card_front)) },
        leadingContent = { Icon(Icons.Outlined.Style, contentDescription = null) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.feature_settings_card_front_summary))
                SegmentedChoices(CardFront.entries, settings.cardFront, onCardFrontChange) { it.label() }
            }
        },
    )
    SwitchRow(
        icon = Icons.Outlined.RecordVoiceOver,
        title = stringResource(R.string.feature_settings_auto_pronounce),
        subtitle = stringResource(R.string.feature_settings_auto_pronounce_summary),
        checked = settings.autoPronounce,
        onCheckedChange = onAutoPronounceChange,
    )
    SwitchRow(
        icon = Icons.Outlined.Shuffle,
        title = stringResource(R.string.feature_settings_shuffle),
        subtitle = stringResource(R.string.feature_settings_shuffle_summary),
        checked = settings.shuffleCards,
        onCheckedChange = onShuffleChange,
    )
}

/** A slider in steps of [NEW_WORDS_STEP], saved when released so dragging doesn't write each value. */
@Composable
private fun NewWordsRow(newWordsPerDay: Int, onChange: (Int) -> Unit) {
    val range = UserSettings.NEW_WORDS_PER_DAY_RANGE
    var value by remember(newWordsPerDay) { mutableFloatStateOf(newWordsPerDay.toFloat()) }
    val shown = value.roundToInt()
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_new_words)) },
        leadingContent = { Icon(Icons.Outlined.WbSunny, contentDescription = null) },
        supportingContent = {
            Column {
                Text(
                    if (shown == 0) {
                        stringResource(R.string.feature_settings_new_words_none)
                    } else {
                        pluralStringResource(R.plurals.feature_settings_new_words_summary, shown, shown)
                    },
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { onChange(value.roundToInt()) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = (range.last - range.first) / NEW_WORDS_STEP - 1,
                )
            }
        },
    )
}

@Composable
private fun DiagnosticsSection(
    diagnostics: DiagnosticsState,
    onRecordingChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    SectionHeader(stringResource(R.string.feature_settings_section_diagnostics))
    SwitchRow(
        icon = Icons.Outlined.BugReport,
        title = stringResource(R.string.feature_settings_diagnostics),
        subtitle = stringResource(R.string.feature_settings_diagnostics_summary),
        checked = diagnostics.isRecording,
        onCheckedChange = onRecordingChange,
    )
    // Only once something has been recorded.
    if (diagnostics.logBytes > 0) {
        val size = Formatter.formatShortFileSize(LocalContext.current, diagnostics.logBytes)
        SettingsRow(
            icon = Icons.Outlined.Description,
            title = stringResource(R.string.feature_settings_diagnostics_export),
            subtitle = stringResource(R.string.feature_settings_diagnostics_export_summary, size),
            onClick = onExport,
        )
        SettingsRow(
            icon = Icons.Outlined.DeleteSweep,
            title = stringResource(R.string.feature_settings_diagnostics_clear),
            subtitle = stringResource(R.string.feature_settings_diagnostics_clear_summary),
            onClick = onClear,
        )
    }
}

@Composable
private fun AboutSection(appInfo: AppInfo) {
    val uriHandler = LocalUriHandler.current
    SectionHeader(stringResource(R.string.feature_settings_section_about))
    SettingsRow(
        icon = Icons.Outlined.Info,
        title = stringResource(R.string.feature_settings_version),
        subtitle = appInfo.versionName,
    )
    SettingsRow(
        icon = Icons.Outlined.Code,
        title = stringResource(R.string.feature_settings_source_code),
        subtitle = appInfo.sourceCodeUrl,
        onClick = { uriHandler.openUri(appInfo.sourceCodeUrl) },
    )
    SettingsRow(
        icon = Icons.AutoMirrored.Outlined.LibraryBooks,
        title = stringResource(R.string.feature_settings_content),
        subtitle = stringResource(R.string.feature_settings_content_summary),
    )
    SettingsRow(
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        title = stringResource(R.string.feature_settings_dictionary),
        subtitle = stringResource(R.string.feature_settings_dictionary_summary),
        onClick = { uriHandler.openUri(WORDNET_URL) },
    )
    SettingsRow(
        icon = Icons.Outlined.Lock,
        title = stringResource(R.string.feature_settings_privacy),
        subtitle = stringResource(R.string.feature_settings_privacy_summary),
    )
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
    )
}

/** A row whose whole surface toggles the switch, announced as a switch. */
@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.feature_settings_theme_system
        ThemeMode.LIGHT -> R.string.feature_settings_theme_light
        ThemeMode.DARK -> R.string.feature_settings_theme_dark
    },
)

@Composable
private fun CardFront.label(): String = stringResource(
    when (this) {
        CardFront.WORD -> R.string.feature_settings_card_front_word
        CardFront.DEFINITION -> R.string.feature_settings_card_front_definition
    },
)

private const val NEW_WORDS_STEP = 5

private const val DIAGNOSTICS_MIME_TYPE = "text/plain"

/** Open English WordNet, credited for the dictionary as its license requires. */
private const val WORDNET_URL = "https://en-word.net/"
