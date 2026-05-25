@file:OptIn(ExperimentalLayoutApi::class)

package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Ряд placeholder'ов для добавления отсутствующих meaning'ов лексемы
 * (translation/definition).
 *
 * @param enabled false → placeholder'ы заблокированы.
 */
@Composable
internal fun AddLexemeMeaningRow(
    canAddTranslation: Boolean,
    canAddDefinition: Boolean,
    enabled: Boolean,
    onCreateTranslation: () -> Unit,
    onCreateDefinition: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .align(Alignment.End),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (canAddTranslation) {
                SubentityChip(
                    labelRes = R.string.word_card_bottom_translation,
                    iconRes = R.drawable.ic_add,
                    enabled = enabled,
                    onClick = onCreateTranslation,
                )
            }
            if (canAddDefinition) {
                SubentityChip(
                    labelRes = R.string.word_card_bottom_definition,
                    iconRes = R.drawable.ic_add,
                    enabled = enabled,
                    onClick = onCreateDefinition,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        AddLexemeMeaningRow(
            canAddTranslation = true,
            canAddDefinition = true,
            enabled = true,
            onCreateTranslation = {},
            onCreateDefinition = {},
        )
    }
}
