package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ImageFlagWidget(
    @DrawableRes flagRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(id = R.string.flag_content_description),
) {
    Image(
        modifier = modifier
            .size(24.dp)
            .border(
                width = 1.dp,
                color = Color.Black,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp)),
        painter = painterResource(id = flagRes),
        contentScale = ContentScale.FillHeight,
        contentDescription = contentDescription
    )
}