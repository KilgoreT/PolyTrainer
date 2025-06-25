@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.wordcard.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.icondropdowned.IconDropdownWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.TopBarState

@Composable
internal fun TopBarWidget(
    topBarState: TopBarState,
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
        title = {},
        actions = {
            IconDropdownWidget(
                isDropDownOpen = topBarState.isMenuOpen,
                onClickDropDown = { sendMessage(Msg.ShowDropdownMenu) },
                onDismissRequest = { sendMessage(Msg.HideDropdownMenu) },
            ) {
                DeleteWordMenuItem {
                    sendMessage(Msg.HideDropdownMenu)
                    sendMessage(Msg.ShowDeleteWordDialog)
                }
            }
        }
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        TopBarWidget(
            topBarState = TopBarState(),
            onBackPress = {},
            sendMessage = {},
        )
    }
}