package me.apomazkin.wordcard.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

/**
 * FAB "Добавить лексему" в Scaffold.floatingActionButton.
 *
 * M3 FAB не имеет нативного `enabled` — disabled эмулируется через alpha + no-op onClick.
 *
 * @param enabled false → FAB полупрозрачный, тапы игнорируются.
 */
@Composable
internal fun AddLexemeWidget(
    enabled: Boolean,
    onAddLexeme: () -> Unit,
) {
    FloatingActionButton(
        modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
        onClick = { if (enabled) onAddLexeme() },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = FloatingActionButtonDefaults.shape,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = R.drawable.ic_add_value),
            contentDescription = stringResource(id = R.string.word_card_add_lexeme),
        )
    }
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.tertiary)
                .padding(16.dp)
        ) {
            AddLexemeWidget(
                enabled = enabled,
                onAddLexeme = {},
            )
        }
    }
}
