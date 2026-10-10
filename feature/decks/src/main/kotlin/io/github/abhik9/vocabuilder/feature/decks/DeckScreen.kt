package io.github.abhik9.vocabuilder.feature.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.DeckOverview
import io.github.abhik9.vocabuilder.core.model.WordSummary
import io.github.abhik9.vocabuilder.core.ui.MasteryLegend
import io.github.abhik9.vocabuilder.core.ui.MasteryPill
import io.github.abhik9.vocabuilder.core.ui.MasteryProgressBar
import io.github.abhik9.vocabuilder.core.ui.label
import io.github.abhik9.vocabuilder.core.ui.title

@Composable
internal fun DeckRoute(
    onBack: () -> Unit,
    viewModel: DeckViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DeckScreen(
        uiState = uiState,
        onBack = onBack,
        onResetProgress = viewModel::resetProgress,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckScreen(
    uiState: DeckUiState,
    onBack: () -> Unit,
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { if (uiState is DeckUiState.Success) Text(uiState.overview.deck.title()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.feature_decks_back),
                        )
                    }
                },
                actions = {
                    if (uiState is DeckUiState.Success) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.feature_decks_more),
                                )
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.feature_decks_reset)) },
                                    leadingIcon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showResetDialog = true
                                    },
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when (uiState) {
            DeckUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            DeckUiState.NotFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.feature_decks_not_found)) }

            is DeckUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "header") { DeckHeader(uiState.overview) }
                item(key = "divider") { HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) }
                items(uiState.words, key = { it.id }) { word -> WordRow(word) }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.feature_decks_reset_title)) },
            text = { Text(stringResource(R.string.feature_decks_reset_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetProgress()
                    },
                ) { Text(stringResource(R.string.feature_decks_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.feature_decks_cancel))
                }
            },
        )
    }
}

@Composable
private fun DeckHeader(overview: DeckOverview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = pluralStringResource(R.plurals.feature_decks_words, overview.counts.total, overview.counts.total) +
                stringResource(R.string.feature_decks_separator) + overview.statusText(),
            style = MaterialTheme.typography.bodyLarge,
        )
        MasteryProgressBar(overview.counts, height = 10.dp)
        MasteryLegend(overview.counts)
        if (overview.isCaughtUp) {
            Text(
                text = stringResource(R.string.feature_decks_caught_up_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WordRow(word: WordSummary) {
    ListItem(
        headlineContent = { Text(word.headword, style = VocabuilderTheme.wordTypography.title) },
        supportingContent = {
            Text(
                text = "${word.partOfSpeech.label(short = true)} ${word.definition}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = { MasteryPill(word.progress) },
    )
}
