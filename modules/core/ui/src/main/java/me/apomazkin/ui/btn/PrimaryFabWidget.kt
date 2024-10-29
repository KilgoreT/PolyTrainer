package me.apomazkin.ui.btn

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeFab
import me.apomazkin.ui.preview.PreviewWidget

/**
 * FAB with primary color.
 */
@Composable
fun PrimaryFabWidget(
    @DrawableRes iconRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    LexemeFab(
        iconRes = iconRes,
        enabled = enabled,
        color = MaterialTheme.colorScheme.primary,
        onClick = onClick,
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        PrimaryFabWidget(
            iconRes = R.drawable.ic_add,
        ) {}
    }
}