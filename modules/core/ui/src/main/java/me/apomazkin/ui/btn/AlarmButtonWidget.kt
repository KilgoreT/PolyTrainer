package me.apomazkin.ui.btn

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeButton
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AlarmButtonWidget(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LexemeButton(
        modifier = modifier,
        titleRes = titleRes,
        enabled = enabled,
        height = 44.dp,
        enabledColor = MaterialTheme.colorScheme.error,
        titleTextColor = MaterialTheme.colorScheme.onError,
        onClick = onClick,
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AlarmButtonWidget(
                titleRes = R.string.button_delete
            ) {}
        }
    }
}