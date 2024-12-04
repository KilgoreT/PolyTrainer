package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.actionBarColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.TopBarState
import me.apomazkin.vocabulary.tools.DataHelper

private const val TOP_BAR_ICON_SIZE = 44

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTopBarWidget(
    state: TopBarState.Action,
    sendMessage: (Msg) -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = actionBarColor,
        ),
        navigationIcon = {
            IconBoxed(
                iconRes = R.drawable.ic_close,
                size = TOP_BAR_ICON_SIZE,
                enabled = true
            ) { sendMessage(Msg.ChangeActionMode(false, null)) }
        },
        title = {
            Text(
                text = state.selectedTermIds.count().toString(),
                style = LexemeStyle.H5,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        },
        actions = {
            if (state.selectedTermIds.size == 1) {
                IconBoxed(
                    iconRes = R.drawable.ic_edit,
                    size = TOP_BAR_ICON_SIZE,
                    enabled = true,
                ) {
                    sendMessage(
                        Msg.StartChangeWord(
                            state.selectedTermIds.first().id,
                            state.selectedTermIds.first().wordValue,
                        )
                    )
                }
            }
            IconBoxed(
                iconRes = R.drawable.ic_move,
                size = TOP_BAR_ICON_SIZE,
                enabled = true,
            ) {}
            IconBoxed(
                iconRes = R.drawable.ic_trash,
                size = 44,
                enabled = true,
            ) {
                sendMessage(
                    Msg.ConfirmDeleteWordDialog(
                        isOpen = true,
                        wordIds = state.selectedTermIds
                    )
                )
            }
        },
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        ActionTopBarWidget(
            state = DataHelper.State.loaded.topBarState.actionState,
        ) {}
    }
}