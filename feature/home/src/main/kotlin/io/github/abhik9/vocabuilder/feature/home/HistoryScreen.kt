package io.github.abhik9.vocabuilder.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.designsystem.component.EmptyState
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.DayActivity
import io.github.abhik9.vocabuilder.core.model.History
import io.github.abhik9.vocabuilder.core.model.WordSummary
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
internal fun HistoryRoute(
    onBack: () -> Unit,
    onOpenWord: (wordId: String) -> Unit,
    onStudyTrouble: () -> Unit,
    viewModel: HistoryViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState, onBack, onOpenWord, onStudyTrouble)
}

/** The learner's streak, their answers of the last four weeks and the words they miss most. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onOpenWord: (wordId: String) -> Unit,
    onStudyTrouble: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_home_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_home_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                HistoryUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is HistoryUiState.Success -> if (uiState.history.totalAnswers == 0) {
                    EmptyState(
                        emoji = "📈",
                        title = stringResource(R.string.feature_home_history_empty_title),
                        message = stringResource(R.string.feature_home_history_empty),
                    )
                } else {
                    HistoryContent(uiState.history, onOpenWord, onStudyTrouble)
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(history: History, onOpenWord: (String) -> Unit, onStudyTrouble: () -> Unit) {
    CardColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.feature_home_current_streak),
                days = history.currentStreak,
                highlighted = true,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.feature_home_longest_streak),
                days = history.longestStreak,
                highlighted = false,
                modifier = Modifier.weight(1f),
            )
        }
        RecentDaysCard(history)
        HomeCard {
            val answers = history.totalAnswers
            val words = history.studiedWords
            Text(
                text = pluralStringResource(R.plurals.feature_home_total_answers, answers, answers),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = pluralStringResource(R.plurals.feature_home_studied_words, words, words),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (history.mostMissed.isNotEmpty()) MostMissedCard(history.mostMissed, onOpenWord, onStudyTrouble)
    }
}

/** A streak as a big number of days, the current one stressed. */
@Composable
private fun StatTile(label: String, days: Int, highlighted: Boolean, modifier: Modifier = Modifier) {
    val colors = if (highlighted) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    }
    Card(modifier = modifier, colors = colors) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (highlighted) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
            Text(days.toString(), style = MaterialTheme.typography.displaySmall)
            Text(pluralStringResource(R.plurals.feature_home_days, days), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * The answers of each of the last four weeks' days as bars, known at the bottom and missed on top. Tapping a bar shows
 * its day's numbers in place of the summary above the chart.
 */
@Composable
private fun RecentDaysCard(history: History) {
    var selected by remember { mutableStateOf<DayActivity?>(null) }
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    HomeCard {
        Text(stringResource(R.string.feature_home_recent_weeks), style = MaterialTheme.typography.titleLarge)
        val day = selected
        Text(
            text = if (day != null) {
                day.description(dateFormat)
            } else {
                recentSummary(history.recentAnswers, history.recentKnew)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Legend()
        Column {
            DayBars(
                days = history.recentDays,
                selected = selected,
                onSelect = { tapped -> selected = tapped.takeIf { it != selected } },
                dateFormat = dateFormat,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    text = history.recentDays.first().day.format(dateFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.feature_home_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun recentSummary(answers: Int, knew: Int): String {
    val count = pluralStringResource(R.plurals.feature_home_recent_answers, answers, answers)
    if (answers == 0) return count
    return stringResource(R.string.feature_home_recent_accuracy, count, (knew * 100f / answers).roundToInt())
}

@Composable
private fun DayActivity.description(dateFormat: DateTimeFormatter): String =
    pluralStringResource(R.plurals.feature_home_day_answers, answers, day.format(dateFormat), answers, knew)

@Composable
private fun Legend() {
    val colors = VocabuilderTheme.answerColors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(colors.knew, stringResource(R.string.feature_home_knew))
        LegendItem(colors.missed, stringResource(R.string.feature_home_missed))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayBars(
    days: List<DayActivity>,
    selected: DayActivity?,
    onSelect: (DayActivity) -> Unit,
    dateFormat: DateTimeFormatter,
) {
    val colors = VocabuilderTheme.answerColors
    val most = days.maxOf { it.answers }.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val description = day.description(dateFormat)
            val isSelected = day == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                    )
                    .clickable(role = Role.Button) { onSelect(day) }
                    .semantics {
                        contentDescription = description
                        this.selected = isSelected
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
            ) {
                // The rounded end is the top of the bar; the bottom sits on the baseline.
                if (day.missed > 0) Segment(day.missed, most, colors.missed, rounded = true)
                if (day.knew > 0) Segment(day.knew, most, colors.knew, rounded = day.missed == 0)
            }
        }
    }
}

@Composable
private fun ColumnScope.Segment(count: Int, most: Int, color: Color, rounded: Boolean) {
    val corner = if (rounded) 4.dp else 0.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((CHART_HEIGHT * (count.toFloat() / most)).coerceAtLeast(MIN_SEGMENT_HEIGHT))
            .background(color, RoundedCornerShape(topStart = corner, topEnd = corner)),
    )
}

@Composable
private fun MostMissedCard(words: List<WordSummary>, onOpenWord: (String) -> Unit, onStudyTrouble: () -> Unit) {
    HomeCard {
        Text(stringResource(R.string.feature_home_most_missed), style = MaterialTheme.typography.titleLarge)
        Column {
            words.forEach { word ->
                ListItem(
                    headlineContent = { Text(word.headword, style = VocabuilderTheme.wordTypography.title) },
                    supportingContent = { Text(word.definition, maxLines = 1) },
                    trailingContent = {
                        Text(
                            text = pluralStringResource(
                                R.plurals.feature_home_missed_times,
                                word.progress.lapseCount,
                                word.progress.lapseCount,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(role = Role.Button) { onOpenWord(word.id) },
                )
            }
        }
        if (words.any { it.progress.isTrouble }) {
            FilledTonalButton(onClick = onStudyTrouble, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_home_study_trouble))
            }
        }
    }
}

private val CHART_HEIGHT = 120.dp
private val MIN_SEGMENT_HEIGHT = 3.dp
