package me.apomazkin.ui.btn

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeButton
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun SecondaryButtonWidget(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    enabled: Boolean = false,
    onClick: () -> Unit,
) {
    LexemeButton(
        modifier = modifier,
        titleRes = titleRes,
        enabled = enabled,
        height = 44.dp,
        enabledColor = MaterialTheme.colorScheme.secondary,
        titleTextColor = MaterialTheme.colorScheme.onSecondary,
        onClick = onClick,
    )
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            SecondaryButtonWidget(
                titleRes = R.string.word_card_added_field,
                enabled = enabled,
            ) {}
        }
    }
}