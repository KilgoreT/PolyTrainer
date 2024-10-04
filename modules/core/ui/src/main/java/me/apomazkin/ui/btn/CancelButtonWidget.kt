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
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeButton
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun CancelButtonWidget(
    @StringRes titleRes: Int = R.string.button_cancel,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LexemeButton(
        titleRes = titleRes,
        enabled = enabled,
        height = 44.dp,
        enabledColor = MaterialTheme.colorScheme.secondary,
        titleTextColor = blackColor,
        onClick = onClick,
    )
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CancelButtonWidget(
                titleRes = R.string.button_cancel
            ) {}
        }
    }
}