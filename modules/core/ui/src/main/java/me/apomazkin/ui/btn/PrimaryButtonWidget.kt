package me.apomazkin.ui.btn

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeButton
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun PrimaryButtonWidget(
    @StringRes titleRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LexemeButton(
        titleRes = titleRes,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth(),
        height = 56.dp,
        enabledColor = MaterialTheme.colorScheme.primary,
        titleTextColor = MaterialTheme.colorScheme.onPrimary,
        onClick = onClick
    )
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        PrimaryButtonWidget(
            titleRes = R.string.logo_title,
            enabled = enabled
        ) {}
    }
}