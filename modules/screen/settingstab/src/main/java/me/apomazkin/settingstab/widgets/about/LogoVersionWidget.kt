package me.apomazkin.settingstab.widgets.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.settingstab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget


private val defaultTextStyle = LexemeStyle.BodyM
private val defaultColor = grayTextColor
private const val DEFAULT_SPACE = 8

@Composable
fun LogoVersionWidget(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int,
    @StringRes titleRes: Int,
    versionTitle: String,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            space = DEFAULT_SPACE.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = modifier
                .size(112.dp)
                .aspectRatio(1f),
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 4.dp,
            color = Color.Black,
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = stringResource(id = titleRes)
            )
        }
        Text(
                text = versionTitle,
                style = defaultTextStyle,
                color = defaultColor,
        )
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(whiteColor)
        ) {
            LogoVersionWidget(
                imageRes = R.drawable.ic_logo,
                titleRes = R.string.logo_title,
                versionTitle = "v1.0.0",
            )
        }
    }
}