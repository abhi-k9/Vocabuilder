package io.github.abhik9.vocabuilder.feature.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.designsystem.component.SectionHeader
import io.github.abhik9.vocabuilder.core.model.Deck
import io.github.abhik9.vocabuilder.core.model.DeckOverview
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.ui.MasteryLegend
import io.github.abhik9.vocabuilder.core.ui.MasteryProgressBar
import io.github.abhik9.vocabuilder.core.ui.colors
import io.github.abhik9.vocabuilder.core.ui.heading
import io.github.abhik9.vocabuilder.core.ui.masteredText
import io.github.abhik9.vocabuilder.core.ui.shortTitle

@Composable
internal fun DecksRoute(
    onDeckClick: (Deck) -> Unit,
    viewModel: DecksViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DecksScreen(uiState = uiState, onDeckClick = onDeckClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DecksScreen(
    uiState: DecksUiState,
    onDeckClick: (Deck) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.feature_decks_title)) }, scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        when (uiState) {
            DecksUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is DecksUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                uiState.packs.forEach { section ->
                    item(key = "pack-${section.pack.id}") { PackHeader(section) }
                    section.levels.forEach { level ->
                        item(key = "level-${section.pack.id}-${level.level}") {
                            SectionHeader(level.level.heading(), modifier = Modifier.padding(top = 8.dp))
                        }
                        items(level.decks, key = { it.deck.id }) { overview ->
                            DeckCard(overview = overview, onClick = { onDeckClick(overview.deck) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackHeader(section: PackSection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(section.pack.name, style = MaterialTheme.typography.headlineSmall)
            Text(section.counts.masteredText(), style = MaterialTheme.typography.bodyMedium)
            MasteryProgressBar(section.counts, height = 10.dp)
            MasteryLegend(section.counts)
        }
    }
}

@Composable
private fun DeckCard(overview: DeckOverview, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(overview.deck.shortTitle(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = overview.statusText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MasteryProgressBar(overview.counts)
            }
            if (overview.isComplete) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.feature_decks_complete_icon),
                    tint = MasteryState.MASTERED.colors().accent,
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

/** "5 to review · 20 new", "All caught up" or "Mastered". */
@Composable
internal fun DeckOverview.statusText(): String = when {
    isComplete -> stringResource(R.string.feature_decks_complete)

    isCaughtUp -> stringResource(R.string.feature_decks_caught_up)

    else -> listOfNotNull(
        dueCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.feature_decks_due, it, it) },
        counts.new.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.feature_decks_new, it, it) },
    ).joinToString(stringResource(R.string.feature_decks_separator))
}
