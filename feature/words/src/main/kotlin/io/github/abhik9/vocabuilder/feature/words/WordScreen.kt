package io.github.abhik9.vocabuilder.feature.words

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.abhik9.vocabuilder.core.designsystem.component.Pill
import io.github.abhik9.vocabuilder.core.designsystem.theme.MaxContentWidth
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.DictionarySource
import io.github.abhik9.vocabuilder.core.model.Sense
import io.github.abhik9.vocabuilder.core.model.WordProgress
import io.github.abhik9.vocabuilder.core.ui.BackButton
import io.github.abhik9.vocabuilder.core.ui.MasteryPill
import io.github.abhik9.vocabuilder.core.ui.PronounceButton
import io.github.abhik9.vocabuilder.core.ui.StarButton
import io.github.abhik9.vocabuilder.core.ui.WordMeaningSheet
import io.github.abhik9.vocabuilder.core.ui.label
import io.github.abhik9.vocabuilder.core.ui.title
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun WordRoute(
    onBack: () -> Unit,
    onOpenWord: (wordId: String) -> Unit,
    viewModel: WordViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canPronounce by viewModel.canPronounce.collectAsStateWithLifecycle()
    val lookedUp by viewModel.lookedUp.collectAsStateWithLifecycle()
    WordScreen(
        uiState = uiState,
        canPronounce = canPronounce,
        onPronounce = viewModel::pronounce,
        onBack = onBack,
        onOpenWord = onOpenWord,
        onLookUp = viewModel::lookUp,
        onToggleStar = viewModel::toggleStar,
        onToggleKnown = viewModel::toggleKnown,
    )
    lookedUp?.let {
        WordMeaningSheet(
            word = it.word,
            meanings = it.entry?.senses.orEmpty(),
            fromWordNet = it.entry?.source == DictionarySource.WORDNET,
            canPronounce = canPronounce,
            onPronounce = viewModel::pronounceLookedUp,
            onDismiss = viewModel::dismissLookedUp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordScreen(
    uiState: WordUiState,
    canPronounce: Boolean,
    onPronounce: () -> Unit,
    onBack: () -> Unit,
    onOpenWord: (wordId: String) -> Unit,
    onLookUp: (word: String) -> Unit,
    onToggleStar: () -> Unit,
    onToggleKnown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (uiState is WordUiState.Success) {
                        StarButton(starred = uiState.word.progress.starred, onToggle = onToggleStar)
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
                WordUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                WordUiState.NotFound -> Text(
                    text = stringResource(R.string.feature_words_not_found),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )

                is WordUiState.Success -> WordContent(
                    state = uiState,
                    canPronounce = canPronounce,
                    onPronounce = onPronounce,
                    onOpenWord = onOpenWord,
                    onLookUp = onLookUp,
                    onToggleKnown = onToggleKnown,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordContent(
    state: WordUiState.Success,
    canPronounce: Boolean,
    onPronounce: () -> Unit,
    onOpenWord: (String) -> Unit,
    onLookUp: (String) -> Unit,
    onToggleKnown: () -> Unit,
) {
    val word = state.word.word
    Column(
        modifier = Modifier
            .widthIn(max = MaxContentWidth)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = word.headword,
                    style = VocabuilderTheme.wordTypography.display,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (canPronounce) PronounceButton(onPronounce)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MasteryPill(state.word.progress)
                state.deck?.let { deck ->
                    Pill(
                        text = deck.title(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        word.senses.forEachIndexed { index, sense -> SenseBlock(index + 1, sense, numbered = word.senses.size > 1) }

        RelatedWords(
            title = stringResource(R.string.feature_words_synonyms),
            words = word.synonyms,
            wordIds = state.relatedWordIds,
            onOpenWord = onOpenWord,
            onLookUp = onLookUp,
        )
        RelatedWords(
            title = stringResource(R.string.feature_words_antonyms),
            words = word.antonyms,
            wordIds = state.relatedWordIds,
            onOpenWord = onOpenWord,
            onLookUp = onLookUp,
        )

        word.note?.let { note ->
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            }
        }

        ProgressCard(state.word.progress, state.today, onToggleKnown)
    }
}

@Composable
private fun SenseBlock(number: Int, sense: Sense, numbered: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (numbered) Text("$number.", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = sense.partOfSpeech.label(),
                style = MaterialTheme.typography.labelLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(sense.definition, style = MaterialTheme.typography.titleMedium)
            Text(
                text = sense.example,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Related words as chips: those that are words of the app open their page, the others show their meanings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelatedWords(
    title: String,
    words: List<String>,
    wordIds: Map<String, String>,
    onOpenWord: (String) -> Unit,
    onLookUp: (String) -> Unit,
) {
    if (words.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            words.forEach { related ->
                val id = wordIds[related]
                if (id != null) {
                    AssistChip(onClick = { onOpenWord(id) }, label = { Text(related) })
                } else {
                    AssistChip(
                        onClick = { onLookUp(related) },
                        label = { Text(related) },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: WordProgress, today: LocalDate, onToggleKnown: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_words_progress), style = MaterialTheme.typography.titleMedium)
            Text(progress.statusText(today), style = MaterialTheme.typography.bodyMedium)
            if (!progress.isNew) {
                Text(
                    text = stringResource(
                        R.string.feature_words_answered_and_missed,
                        pluralStringResource(
                            R.plurals.feature_words_answered,
                            progress.reviewCount,
                            progress.reviewCount,
                        ),
                        pluralStringResource(R.plurals.feature_words_missed, progress.lapseCount, progress.lapseCount),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onToggleKnown) {
                val label = if (progress.alreadyKnown) {
                    R.string.feature_words_unmark_known
                } else {
                    R.string.feature_words_mark_known
                }
                Text(stringResource(label))
            }
        }
    }
}

@Composable
private fun WordProgress.statusText(today: LocalDate): String {
    val due = dueDay
    return when {
        alreadyKnown -> stringResource(R.string.feature_words_status_known)

        isNew || due == null -> stringResource(R.string.feature_words_status_new)

        !due.isAfter(today) -> stringResource(R.string.feature_words_status_due_today)

        else -> stringResource(
            R.string.feature_words_status_due,
            due.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
        )
    }
}
