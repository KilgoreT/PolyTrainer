@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.quiz.chat.widget.appbar

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import me.apomazkin.core_resources.R
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
                // IS500: сабтайтл с охватом тренировки — ВСЕГДА
                // (решение прогона 2026-09-26): имя группы либо «Все».
                Column {
                    Text(
                            text = stringResource(id = R.string.chat_quiz_app_bar_title),
                            style = LexemeStyle.H5,
                    )
                    Text(
                            text = state.quizGroupName
                                    ?: stringResource(id = R.string.group_all_title),
                            style = LexemeStyle.BodyS.copy(color = grayTextColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                    )
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
