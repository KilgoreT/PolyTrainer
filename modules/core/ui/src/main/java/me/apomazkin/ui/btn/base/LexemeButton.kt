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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

private const val DEFAULT_ROUNDED_CORNER = 12
private const val DEFAULT_HORIZONTAL_PADDING = 28

@Composable
fun LexemeButton(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    height: Dp,
    enabledColor: Color,
    titleTextColor: Color,
    enabled: Boolean = false,
    horizontalPadding: Int = DEFAULT_HORIZONTAL_PADDING,
    disabledColor: Color = MaterialTheme.colorScheme.secondary,
    titleTextStyle: TextStyle = LexemeStyle.BodyM,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
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
            modifier = modifier
                .padding(horizontal = horizontalPadding.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = titleTextStyle,
                color = titleTextColor,
            )
        }
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun PreviewAlarm() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LexemeButton(
                titleRes = R.string.button_delete,
                enabled = false,
                height = 44.dp,
                enabledColor = MaterialTheme.colorScheme.error,
                titleTextColor = MaterialTheme.colorScheme.onError,
            ) {}
        }
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun PreviewCancel() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LexemeButton(
                titleRes = R.string.button_delete,
                enabled = false,
                height = 44.dp,
                enabledColor = MaterialTheme.colorScheme.secondary,
                titleTextColor = blackColor,
            ) {}
        }
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun PreviewWide() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LexemeButton(
                modifier = Modifier
                    .fillMaxWidth(),
                titleRes = R.string.button_delete,
                enabled = false,
                height = 56.dp,
                enabledColor = MaterialTheme.colorScheme.primary,
                titleTextColor = MaterialTheme.colorScheme.onPrimary,
            ) {}
        }
    }
}