package me.apomazkin.dictionary.form.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionary.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

private val BUTTON_HEIGHT = 56.dp
private val BUTTON_SHAPE = RoundedCornerShape(16.dp)

// Compose elevation ≠ Figma blur 22 — стартовое значение, подбирается на девайсе.
private val SHADOW_ELEVATION = 10.dp

/**
 * Кнопка подтверждения формы (Figma 5027:1235-1237): radius 16, цветная тень
 * от primary при enabled; disabled — приглушённый контейнер темы без тени.
 */
@Composable
internal fun SubmitButtonWidget(
    @StringRes titleRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (enabled) it.shadow(
                    elevation = SHADOW_ELEVATION,
                    shape = BUTTON_SHAPE,
                    ambientColor = LexemeColor.primary.copy(alpha = 0.5f),
                    spotColor = LexemeColor.primary.copy(alpha = 0.5f),
                ) else it
            }
            .height(BUTTON_HEIGHT),
        shape = BUTTON_SHAPE,
        colors = ButtonDefaults.buttonColors(
            containerColor = LexemeColor.primary,
            contentColor = whiteColor,
            disabledContainerColor = MaterialTheme.colorScheme.onSecondary,
            disabledContentColor = MaterialTheme.colorScheme.secondary,
        ),
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(id = titleRes),
            style = LexemeStyle.BodyLBold,
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewEnabled() {
    AppTheme {
        SubmitButtonWidget(
            titleRes = R.string.dictionary_create,
            enabled = true,
            onClick = {},
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewDisabled() {
    AppTheme {
        SubmitButtonWidget(
            titleRes = R.string.dictionary_create,
            enabled = false,
            onClick = {},
        )
    }
}
