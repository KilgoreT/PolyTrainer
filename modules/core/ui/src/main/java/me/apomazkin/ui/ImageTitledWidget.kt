package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.Typography


private val DEFAULT_TEXT_STYLE = Typography.headlineSmall
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
    textStyle: TextStyle = DEFAULT_TEXT_STYLE,
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
            style = textStyle
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun Preview() {
    AppTheme {
        ImageTitledWidget(
            imageRes = R.drawable.ic_splash_logo,
            titleRes = R.string.logo_title
        )
    }
}