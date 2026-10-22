package io.github.abhik9.vocabuilder.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.DeckOverview
import io.github.abhik9.vocabuilder.core.model.TodayOverview
import io.github.abhik9.vocabuilder.core.ui.MasteryLegend
import io.github.abhik9.vocabuilder.core.ui.MasteryProgressBar
import io.github.abhik9.vocabuilder.core.ui.title

@Composable
internal fun HomeRoute(
    onStartDailyReview: (quiz: Boolean) -> Unit,
    onStudyDeck: (Deck) -> Unit,
    onOpenDecks: () -> Unit,
    onStudyStarred: () -> Unit,
    onStudyTrouble: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onStartDailyReview = onStartDailyReview,
        onStudyDeck = onStudyDeck,
        onOpenDecks = onOpenDecks,
        onStudyStarred = onStudyStarred,
        onStudyTrouble = onStudyTrouble,
        onOpenHistory = onOpenHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onStartDailyReview: (quiz: Boolean) -> Unit,
    onStudyDeck: (Deck) -> Unit,
    onOpenDecks: () -> Unit,
    onStudyStarred: () -> Unit,
    onStudyTrouble: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_home_title)) }) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                HomeUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is HomeUiState.Success -> CardColumn {
                    DailyReviewCard(uiState.today, onStartDailyReview)
                    uiState.today.nextDeck?.let { ContinueCard(it, onStudyDeck) }
                    if (uiState.today.starredCount > 0) {
                        StudySetCard(
                            icon = Icons.Filled.Star,
                            text = pluralStringResource(
                                R.plurals.feature_home_starred,
                                uiState.today.starredCount,
                                uiState.today.starredCount,
                            ),
                            onStudy = onStudyStarred,
                        )
                    }
                    if (uiState.today.troubleCount > 0) {
                        StudySetCard(
                            icon = Icons.Filled.FitnessCenter,
                            text = pluralStringResource(
                                R.plurals.feature_home_trouble,
                                uiState.today.troubleCount,
                                uiState.today.troubleCount,
                            ),
                            onStudy = onStudyTrouble,
                        )
                    }
                    ProgressCard(uiState.today, onOpenDecks, onOpenHistory)
                }
            }
        }
    }
}

@Composable
private fun DailyReviewCard(today: TodayOverview, onStart: (quiz: Boolean) -> Unit) {
    HomeCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(stringResource(R.string.feature_home_daily_review), style = MaterialTheme.typography.titleLarge)
        if (today.dailyReviewCount > 0) {
            Text(today.dailyReviewText(), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onStart(false) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_home_start))
                }
                OutlinedButton(onClick = { onStart(true) }) {
                    Text(stringResource(R.string.feature_home_quiz))
                }
            }
        } else {
            Text(stringResource(R.string.feature_home_daily_done), style = MaterialTheme.typography.bodyLarge)
        }
        if (today.streak > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null)
                Text(
                    text = pluralStringResource(R.plurals.feature_home_streak, today.streak, today.streak),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        if (today.reviewedToday > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.feature_home_reviewed_today,
                    today.reviewedToday,
                    today.reviewedToday,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** "12 words to review and 10 new words". */
@Composable
private fun TodayOverview.dailyReviewText(): String {
    val due = pluralStringResource(R.plurals.feature_home_due, dueCount, dueCount)
    val new = pluralStringResource(R.plurals.feature_home_new, newCount, newCount)
    return when {
        dueCount > 0 && newCount > 0 -> stringResource(R.string.feature_home_and, due, new)
        dueCount > 0 -> due
        else -> new
    }
}

@Composable
private fun ContinueCard(overview: DeckOverview, onStudyDeck: (Deck) -> Unit) {
    HomeCard {
        Text(
            text = stringResource(R.string.feature_home_continue_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(overview.deck.title(), style = MaterialTheme.typography.titleLarge)
        MasteryProgressBar(overview.counts)
        FilledTonalButton(onClick = { onStudyDeck(overview.deck) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.feature_home_continue))
        }
    }
}

/** A set of words to study apart from the decks: the starred words, or the trouble words. */
@Composable
private fun StudySetCard(icon: ImageVector, text: String, onStudy: () -> Unit) {
    HomeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onStudy) { Text(stringResource(R.string.feature_home_study)) }
        }
    }
}

@Composable
private fun ProgressCard(today: TodayOverview, onOpenDecks: () -> Unit, onOpenHistory: () -> Unit) {
    HomeCard {
        Text(stringResource(R.string.feature_home_progress_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = pluralStringResource(
                R.plurals.feature_home_progress,
                today.counts.total,
                today.counts.mastered,
                today.counts.total,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        MasteryProgressBar(today.counts, height = 10.dp)
        MasteryLegend(today.counts)
        Row(modifier = Modifier.align(Alignment.End)) {
            TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.feature_home_history)) }
            TextButton(onClick = onOpenDecks) { Text(stringResource(R.string.feature_home_all_decks)) }
        }
    }
}
