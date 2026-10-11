package io.github.abhik9.vocabuilder.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.Meaning

/** A word's meanings, one after the other in the enclosing column, numbered if there are several. */
@Composable
fun Senses(senses: List<Meaning>) {
    senses.forEachIndexed { index, sense ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (senses.size > 1) {
                Text(
                    text = stringResource(R.string.core_ui_sense_number, index + 1),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = sense.partOfSpeech.label(),
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = sense.definition, style = MaterialTheme.typography.bodyLarge)
                sense.example?.let { example ->
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * A synonym or antonym looked up from a word: its meanings, from its own card if it's a word of the app or else from
 * the dictionary, with a button to hear it.
 *
 * @param fromWordNet credits Open English WordNet, as its license requires, when the meanings come from it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordMeaningSheet(
    word: String,
    meanings: List<Meaning>,
    fromWordNet: Boolean,
    canPronounce: Boolean,
    onPronounce: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = word,
                    style = VocabuilderTheme.wordTypography.title,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (canPronounce) {
                    IconButton(onClick = onPronounce) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.core_ui_pronounce),
                        )
                    }
                }
            }
            if (meanings.isEmpty()) {
                Text(
                    text = stringResource(R.string.core_ui_no_meaning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Senses(meanings)
            }
            if (fromWordNet) {
                Text(
                    text = stringResource(R.string.core_ui_wordnet_credit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
