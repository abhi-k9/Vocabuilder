package io.github.abhik9.vocabuilder.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onStartDailyReview: () -> Unit,
    onStudyDeck: (Deck) -> Unit,
    onOpenDecks: () -> Unit,
    onStudyStarred: () -> Unit,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onStartDailyReview = onStartDailyReview,
        onStudyDeck = onStudyDeck,
        onOpenDecks = onOpenDecks,
        onStudyStarred = onStudyStarred,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onStartDailyReview: () -> Unit,
    onStudyDeck: (Deck) -> Unit,
    onOpenDecks: () -> Unit,
    onStudyStarred: () -> Unit,
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

                is HomeUiState.Success -> Column(
                    modifier = Modifier
                        .widthIn(max = MAX_CONTENT_WIDTH)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DailyReviewCard(uiState.today, onStartDailyReview)
                    uiState.today.nextDeck?.let { ContinueCard(it, onStudyDeck) }
                    if (uiState.today.starredCount > 0) StarredCard(uiState.today.starredCount, onStudyStarred)
                    ProgressCard(uiState.today, onOpenDecks)
                }
            }
        }
    }
}

@Composable
private fun DailyReviewCard(today: TodayOverview, onStart: () -> Unit) {
    HomeCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(stringResource(R.string.feature_home_daily_review), style = MaterialTheme.typography.titleLarge)
        if (today.dailyReviewCount > 0) {
            Text(today.dailyReviewText(), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_home_start))
            }
        } else {
            Text(stringResource(R.string.feature_home_daily_done), style = MaterialTheme.typography.bodyLarge)
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

@Composable
private fun StarredCard(count: Int, onStudyStarred: () -> Unit) {
    HomeCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(
                text = pluralStringResource(R.plurals.feature_home_starred, count, count),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = onStudyStarred) { Text(stringResource(R.string.feature_home_study)) }
        }
    }
}

@Composable
private fun ProgressCard(today: TodayOverview, onOpenDecks: () -> Unit) {
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
        TextButton(onClick = onOpenDecks, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.feature_home_all_decks))
        }
    }
}

@Composable
private fun HomeCard(
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = colors) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private val MAX_CONTENT_WIDTH = 640.dp
