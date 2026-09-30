package me.apomazkin.quiz.chat.widget.button.base

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.widget.ChatMotion
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.chatMessageBtnBorder
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ChatButtonWidget(
    modifier: Modifier = Modifier,
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    // Геометрия — ровно пузыря юзера (ChatMotion.BUBBLE_*: те же углы, те
    // же отступы, тот же стиль BodyM): кнопка занимает место будущего
    // пузыря, и при замене стопка выше не оседает. Не кликабельный
    // Surface(onClick): тот раздувает раскладку до минимальной зоны касания
    // 48dp, рисуя фон по содержимому, — ряд чипов был выше пузыря на 18dp
    // при одинаковом виде.
    val shape = RoundedCornerShape(
        topStart = ChatMotion.BUBBLE_CORNER,
        topEnd = ChatMotion.BUBBLE_CORNER,
        bottomStart = ChatMotion.BUBBLE_CORNER,
        bottomEnd = ChatMotion.BUBBLE_TAIL_CORNER,
    )
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        border = BorderStroke(width = 1.dp, color = chatMessageBtnBorder),
        color = whiteColor,
    ) {
        Box(
            modifier = Modifier
                .padding(
                    horizontal = ChatMotion.BUBBLE_PADDING_H,
                    vertical = ChatMotion.BUBBLE_PADDING_V,
                ),
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