package me.apomazkin.quiz.chat.widget.message

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AvatarWidget(
    modifier: Modifier = Modifier,
    @DrawableRes avatarRes: Int,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color = Color.Black),
    ) {
        Image(
            modifier = Modifier
                .size(48.dp),
            painter = painterResource(avatarRes),
            contentDescription = "",
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AvatarWidget(
        avatarRes = R.drawable.ic_logo,
    )
}