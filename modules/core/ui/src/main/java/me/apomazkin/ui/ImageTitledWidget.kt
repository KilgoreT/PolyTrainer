package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.Typography
import me.apomazkin.ui.preview.PreviewWidget


private val defaultTextStyle = Typography.headlineSmall
private val defaultColor = Color.Unspecified
private const val DEFAULT_SPACE = 8

/**
 * Image with Text below.
 * Horizontal centered.
 * Text in 1 line.
 */
@Composable
fun ImageTitledWidget(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int,
    @StringRes titleRes: Int,
    textStyle: TextStyle = defaultTextStyle,
    textColor: Color = defaultColor,
    spaceBetween: Int = DEFAULT_SPACE,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            space = spaceBetween.dp,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = stringResource(id = titleRes)
        )
        Text(
            text = stringResource(id = titleRes),
            maxLines = 1,
            style = textStyle,
            color = textColor,
        )
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
        ) {
            ImageTitledWidget(
                imageRes = R.drawable.ic_logo,
                titleRes = R.string.logo_title,
                textColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}