package me.apomazkin.wordcard.widget.lexeme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Округлый чип с подписью и иконкой справа.
 *
 * @param labelRes подпись.
 * @param iconRes иконка справа.
 * @param enabled блокирует клик.
 * @param onClick тап по чипу.
 */
@Composable
internal fun SubentityChip(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            modifier = Modifier.padding(
                PaddingValues(start = 12.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewPlaceholderTranslation() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            SubentityChip(
                labelRes = R.string.word_card_bottom_translation,
                iconRes = R.drawable.ic_add,
                enabled = true,
                onClick = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewPlaceholderDefinition() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            SubentityChip(
                labelRes = R.string.word_card_bottom_definition,
                iconRes = R.drawable.ic_add,
                enabled = true,
                onClick = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewActiveTranslation() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            SubentityChip(
                labelRes = R.string.word_card_bottom_translation,
                iconRes = R.drawable.ic_close,
                enabled = true,
                onClick = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewActiveDefinition() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            SubentityChip(
                labelRes = R.string.word_card_bottom_definition,
                iconRes = R.drawable.ic_close,
                enabled = true,
                onClick = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewDisabled() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            SubentityChip(
                labelRes = R.string.word_card_lexeme_add_translation,
                iconRes = R.drawable.ic_add,
                enabled = false,
                onClick = {},
            )
        }
    }
}
