@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.quiz.chat.widget.appbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.quiz.chat.logic.AppBarState
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.enableIconColor
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
                Text(
                        text = stringResource(id = R.string.chat_quiz_app_bar_title),
                        style = LexemeStyle.H5,
                )
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
