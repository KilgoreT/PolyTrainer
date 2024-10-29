package me.apomazkin.ui.btn.base

import androidx.annotation.StringRes
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.disableButtonTitleColor
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.PreviewWidget

private val containerColor = Color.Transparent

@Composable
fun LexemeTextButton(
    @StringRes title: Int,
    enabled: Boolean = false,
    contentColor: Color,
    disabledContentColor: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            disabledContentColor = disabledContentColor,
            containerColor = containerColor,
            disabledContainerColor = containerColor,
        ),
    ) {
        Text(
            text = stringResource(id = title),
            style = LexemeStyle.BodyM,
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewEnabled() {
    AppTheme {
        LexemeTextButton(
            title = R.string.logo_title,
            enabled = true,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = disableButtonTitleColor
        ) {}
    }
}

@Composable
@PreviewWidget
private fun PreviewDisabled() {
    AppTheme {
        LexemeTextButton(
            title = R.string.logo_title,
            enabled = false,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = disableButtonTitleColor
        ) {}
    }
}