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
import me.apomazkin.wordcard.mate.TopBarState

@Composable
internal fun TopBarWidget(
    topBarState: TopBarState,
    onBackPress: () -> Unit,
    onOpenMenu: () -> Unit,
    onCloseMenu: () -> Unit,
    onDeleteWord: () -> Unit,
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
                onClickDropDown = onOpenMenu,
                onDismissRequest = onCloseMenu,
            ) {
                DeleteWordMenuItem(onDeleteClick = onDeleteWord)
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
            onOpenMenu = {},
            onCloseMenu = {},
            onDeleteWord = {},
        )
    }
}