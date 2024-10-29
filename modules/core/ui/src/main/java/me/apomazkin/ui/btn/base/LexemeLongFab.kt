@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.ui.btn.base

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
private const val DEFAULT_HORIZONTAL_PADDING = 16
private val defaultHeight = 56.dp

@Composable
fun LexemeLongFab(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int? = null,
    height: Dp = defaultHeight,
    contentColor: Color,
    enabledColor: Color,
    enabled: Boolean = false,
    horizontalPadding: Int = DEFAULT_HORIZONTAL_PADDING,
    disabledColor: Color = MaterialTheme.colorScheme.secondary,
    titleTextStyle: TextStyle = LexemeStyle.BodyL,
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
        Row(
            modifier = Modifier
                .padding(horizontal = horizontalPadding.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            iconRes?.let { icon ->
                Icon(
                    painter = painterResource(id = icon),
                    tint = contentColor,
                    contentDescription = stringResource(id = titleRes),
                )
            }
            Text(
                text = stringResource(id = titleRes),
                style = titleTextStyle,
                color = contentColor,
            )
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
            modifier = Modifier
                .background(color = blackColor),
            contentAlignment = Alignment.Center,
        ) {
            LexemeLongFab(
                modifier = Modifier,
                titleRes = R.string.word_card_add_lexeme,
                iconRes = R.drawable.ic_add_value,
                enabled = enabled,
                height = 56.dp,
                contentColor = MaterialTheme.colorScheme.onError,
                enabledColor = MaterialTheme.colorScheme.error,
            ) {}
        }
    }
}