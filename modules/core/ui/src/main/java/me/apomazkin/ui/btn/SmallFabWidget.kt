package me.apomazkin.ui.btn

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeFab
import me.apomazkin.ui.preview.PreviewWidget

private val SMALL_FAB_SIZE = 40.dp

/**
 * IS527: вторичный FAB над основным (камера над «+»). Цвет — тот же
 * primary, что у [PrimaryFabWidget], размер 40 dp. [contentDescription]
 * обязателен: иначе TalkBack читает обе кнопки одинаково.
 */
@Composable
fun SmallFabWidget(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LexemeFab(
        iconRes = iconRes,
        enabled = enabled,
        color = MaterialTheme.colorScheme.primary,
        size = SMALL_FAB_SIZE,
        contentDescription = contentDescription,
        onClick = onClick,
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        SmallFabWidget(
            iconRes = R.drawable.ic_send,
            contentDescription = "Send",
        ) {}
    }
}
