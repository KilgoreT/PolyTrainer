@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.quiz.chat.widget.appbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.quiz.QuizGroupLabel
import me.apomazkin.quiz.chat.logic.AppBarState
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.enableIconColor
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun AppBarWidget(
        state: AppBarState,
        onBackPress: () -> Unit,
        sendMessage: (Msg) -> Unit,
) {
    TopAppBar(
            navigationIcon = {
                IconBoxed(
                        iconRes = R.drawable.ic_back,
                        enabled = true,
                        colorEnabled = enableIconColor,
                        size = 44,
                        onClick = onBackPress,
                )
            },
            title = {
                // Сабтайтл с охватом тренировки — ВСЕГДА: «Все», имя
                // группы либо «Быт +2». Обрезается только имя, «+N» видно
                // всегда — иначе набор неотличим от одной группы.
                Column {
                    Text(
                            text = stringResource(id = R.string.chat_quiz_app_bar_title),
                            style = LexemeStyle.H5,
                    )
                    val subtitleStyle = LexemeStyle.BodyS.copy(color = grayTextColor)
                    val label = state.quizGroupLabel
                    Row {
                        Text(
                                modifier = Modifier.weight(1f, fill = false),
                                text = label?.first
                                        ?: stringResource(id = R.string.group_all_title),
                                style = subtitleStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                        )
                        if (label != null && label.more > 0) {
                            Text(
                                    modifier = Modifier.padding(start = 4.dp),
                                    text = stringResource(id = R.string.quiz_group_label_more, label.more),
                                    style = subtitleStyle,
                                    maxLines = 1,
                            )
                        }
                    }
                }
            },
            actions = {
                ActionsWidget(
                        isActionsOpen = state.isActionMenuOpen,
                        state = state.itemsState,
                        sendMessage = sendMessage,
                )
            }
    )
}

@PreviewWidget
@Composable
fun AppBarWidgetPreview() {
    AppBarWidget(
            state = AppBarState(),
            onBackPress = {},
            sendMessage = {},
    )
}

@PreviewWidget
@Composable
fun AppBarWidgetGroupsPreview() {
    AppBarWidget(
            state = AppBarState(
                    quizGroupLabel = QuizGroupLabel(
                            first = "Очень длинное название группы слов",
                            more = 2,
                    ),
            ),
            onBackPress = {},
            sendMessage = {},
    )
}
