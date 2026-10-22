package io.github.abhik9.vocabuilder.feature.words

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.designsystem.component.EmptyState
import io.github.abhik9.vocabuilder.core.model.MasteryState
import io.github.abhik9.vocabuilder.core.model.WordSummary
import io.github.abhik9.vocabuilder.core.ui.WordSummaryRow
import io.github.abhik9.vocabuilder.core.ui.label

@Composable
internal fun WordsRoute(
    onWordClick: (WordSummary) -> Unit,
    viewModel: WordsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WordsScreen(
        uiState = uiState,
        query = viewModel.query,
        onQueryChange = viewModel::updateQuery,
        onFilterChange = viewModel::setFilter,
        onWordClick = onWordClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordsScreen(
    uiState: WordsUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterChange: (WordFilter) -> Unit,
    onWordClick: (WordSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_words_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.feature_words_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.feature_words_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = MaterialTheme.shapes.extraLarge,
            )
            val selected = (uiState as? WordsUiState.Success)?.filter ?: WordFilter.ALL
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(WordFilter.entries) { filter ->
                    FilterChip(
                        selected = filter == selected,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.label()) },
                    )
                }
            }
            when (uiState) {
                WordsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is WordsUiState.Success -> if (uiState.words.isEmpty()) {
                    EmptyState(
                        emoji = "🔍",
                        title = stringResource(R.string.feature_words_no_results_title),
                        message = stringResource(R.string.feature_words_no_results_message),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "count") {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.feature_words_count,
                                    uiState.words.size,
                                    uiState.words.size,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        items(uiState.words, key = { it.id }) { word ->
                            WordSummaryRow(word = word, onClick = { onWordClick(word) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordFilter.label(): String = when (this) {
    WordFilter.ALL -> stringResource(R.string.feature_words_filter_all)
    WordFilter.STARRED -> stringResource(R.string.feature_words_filter_starred)
    WordFilter.NEW -> MasteryState.NEW.label()
    WordFilter.LEARNING -> MasteryState.LEARNING.label()
    WordFilter.REVIEWING -> MasteryState.REVIEWING.label()
    WordFilter.MASTERED -> MasteryState.MASTERED.label()
}
