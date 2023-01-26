package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme

private const val DEFAULT_SHAPE = 0
private const val DEFAULT_DESCRIPTION = ""

@Composable
fun ImageRoundedWidget(
    @DrawableRes imageRes: Int,
    topStart: Int = DEFAULT_SHAPE,
    topEnd: Int = DEFAULT_SHAPE,
    bottomStart: Int = DEFAULT_SHAPE,
    bottomEnd: Int = DEFAULT_SHAPE,
    contentDescription: String = DEFAULT_DESCRIPTION
) {
    Image(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = topStart.dp,
                    topEnd = topEnd.dp,
                    bottomStart = bottomStart.dp,
                    bottomEnd = bottomEnd.dp,
                )
            ),
        painter = painterResource(id = imageRes),
        contentDescription = contentDescription,
    )
}

@Composable
@Preview
private fun Preview() {
    AppTheme {
        ImageRoundedWidget(
            imageRes = R.drawable.image_lang_selection,
            topStart = 24,
            bottomStart = 24,
        )
    }
}