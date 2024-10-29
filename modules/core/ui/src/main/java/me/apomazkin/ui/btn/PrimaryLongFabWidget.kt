package me.apomazkin.ui.btn

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeLongFab
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun PrimaryLongFabWidget(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    LexemeLongFab(
        modifier = modifier,
        titleRes = titleRes,
        iconRes = iconRes,
        enabled = enabled,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        enabledColor = MaterialTheme.colorScheme.primary,
    ) { onClick.invoke() }
}

@PreviewWidget
@Composable
private fun PreviewPrimaryLongFabWidget(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(color = blackColor),
            contentAlignment = Alignment.Center,
        ) {
            PrimaryLongFabWidget(
                titleRes = R.string.word_card_add_lexeme,
                iconRes = R.drawable.ic_add_value,
                enabled = enabled,
            ) {}
        }
    }
}