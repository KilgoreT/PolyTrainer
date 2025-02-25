package me.apomazkin.ui.btn.base

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_ROUNDED_CORNER = 12
private const val DEFAULT_HORIZONTAL_PADDING = 28

@Composable
fun LexemeButton(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    height: Dp,
    enabledColor: Color,
    titleTextColor: Color,
    disabledTitleTextColor: Color = MaterialTheme.colorScheme.secondary,
    enabled: Boolean = false,
    horizontalPadding: Int = DEFAULT_HORIZONTAL_PADDING,
    disabledColor: Color = MaterialTheme.colorScheme.onSecondary,
    titleTextStyle: TextStyle = LexemeStyle.BodyM,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier
            .height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = enabledColor,
            disabledContainerColor = disabledColor,
        ),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(DEFAULT_ROUNDED_CORNER.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = horizontalPadding.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = titleTextStyle,
                color = if (enabled) titleTextColor else disabledTitleTextColor,
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewAlarm(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center,
        ) {
            LexemeButton(
                titleRes = R.string.button_delete,
                enabled = enabled,
                height = 44.dp,
                enabledColor = MaterialTheme.colorScheme.error,
                titleTextColor = MaterialTheme.colorScheme.onError,
            ) {}
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewCancel(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center,
        ) {
            LexemeButton(
                titleRes = R.string.button_delete,
                enabled = enabled,
                height = 44.dp,
                enabledColor = MaterialTheme.colorScheme.onSecondary,
                titleTextColor = blackColor,
            ) {}
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewWide(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center,
        ) {
            LexemeButton(
                modifier = Modifier
                    .fillMaxWidth(),
                titleRes = R.string.button_delete,
                enabled = enabled,
                height = 56.dp,
                enabledColor = MaterialTheme.colorScheme.primary,
                titleTextColor = MaterialTheme.colorScheme.onPrimary,
            ) {}
        }
    }
}