package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun ImageFlagWidget(
    @DrawableRes flagRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(id = R.string.flag_content_description),
) {
    Image(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape),
        painter = painterResource(id = flagRes),
        contentScale = ContentScale.FillHeight,
        contentDescription = contentDescription
    )
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        ImageFlagWidget(
            flagRes = R.drawable.example_ic_flag_gb,
        )
    }
}