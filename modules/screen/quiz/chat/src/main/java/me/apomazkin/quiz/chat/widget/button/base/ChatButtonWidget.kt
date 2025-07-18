package me.apomazkin.quiz.chat.widget.button.base

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.chatMessageBtnBorder
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ChatButtonWidget(
    @StringRes title: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(48.dp),
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 8.dp,
        ),
        border = BorderStroke(width = 1.dp, color = chatMessageBtnBorder),
        color = whiteColor,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = title),
                style = LexemeStyle.BodyM.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
            )
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray)
        ) {
            ChatButtonWidget(
                title = R.string.button_cancel
            ) {}
        }
    }
}