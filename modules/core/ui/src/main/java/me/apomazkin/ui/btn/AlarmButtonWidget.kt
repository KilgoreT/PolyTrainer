package me.apomazkin.ui.btn

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.dividerColor
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeButton
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AlarmButtonWidget(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    titleOverride: String? = null,
    enabled: Boolean = true,
    /** IS493 Э6: перекраска в деструктивный режим («Удалить всё»). */
    containerColor: Color? = null,
    contentColor: Color? = null,
    height: Dp = 44.dp,
    onClick: () -> Unit,
) {
    LexemeButton(
        modifier = modifier,
        titleRes = titleRes,
        titleOverride = titleOverride,
        enabled = enabled,
        height = height,
        enabledColor = containerColor ?: MaterialTheme.colorScheme.error,
        titleTextColor = contentColor ?: MaterialTheme.colorScheme.onError,
        // IS493 Э6: disabled ОБЯЗАН отличаться визуально (анти-дребезг
        // деструктива) — явный серый, не дефолты темы.
        disabledColor = dividerColor,
        disabledTitleTextColor = grayTextColor,
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