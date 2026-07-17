package me.apomazkin.wordcard.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.fabLavender
import me.apomazkin.theme.fabLavenderDisabled
import me.apomazkin.theme.fabShadowTint
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

/**
 * FAB "Добавить лексему" в Scaffold.floatingActionButton.
 *
 * M3 FAB не имеет нативного `enabled` — disabled эмулируется через alpha + no-op onClick.
 *
 * @param enabled false → FAB полупрозрачный, тапы игнорируются.
 */
private val FAB_SHAPE = RoundedCornerShape(19.dp)

@Composable
internal fun AddLexemeWidget(
    enabled: Boolean,
    onAddLexeme: () -> Unit,
) {
    FloatingActionButton(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.75f)
            .let {
                if (enabled) it.shadow(
                    elevation = 10.dp,
                    shape = FAB_SHAPE,
                    ambientColor = fabShadowTint.copy(alpha = 0.5f),
                    spotColor = fabShadowTint.copy(alpha = 0.5f),
                ) else it
            }
            .size(60.dp),
        onClick = { if (enabled) onAddLexeme() },
        containerColor = if (enabled) fabLavender else fabLavenderDisabled,
        contentColor = LexemeColor.primary,
        shape = FAB_SHAPE,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
    ) {
        Icon(
            modifier = Modifier.size(26.dp),
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
