package me.apomazkin.dictionarytab.ui.widget.topBar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import me.apomazkin.dictionarytab.R
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.logic.TopBarState
import me.apomazkin.dictionarytab.tools.DataHelper
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidget


private const val TOP_BAR_ICON_SIZE = 44

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTopBarWidget(
    state: TopBarState.Action,
    sendMessage: (Msg) -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondary,
        ),
        navigationIcon = {
            IconBoxed(
                iconRes = R.drawable.ic_close,
                size = TOP_BAR_ICON_SIZE,
                enabled = true
            ) { sendMessage(Msg.ExitSelectionMode) }
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
                        Msg.OpenEditWordDialog(
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
                    Msg.OpenDeleteConfirmation(
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