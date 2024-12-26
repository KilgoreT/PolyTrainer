@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.quiztab.widget

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import me.apomazkin.dictionarypicker.DictDropDownWidget
import me.apomazkin.dictionarypicker.state.LangPickerState
import me.apomazkin.quiztab.logic.Msg
import me.apomazkin.quiztab.logic.TopBarActionMsg
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun AppBarWidget(
    state: LangPickerState?,
    openAddDict: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    TopAppBar(
        title = { AppBarTitleWidget() },
        actions = {
            state?.let { langState: LangPickerState ->
                DictDropDownWidget(
                    state = langState,
                    isExpand = langState.isDropDownMenuOpen,
                    openAddDict = openAddDict,
                    onOpenDropDown = {
                        sendMessage(
                            TopBarActionMsg.ExpandDictMenu(
                                expand = true
                            )
                        )
                    },
                    onDismiss = {
                        sendMessage(
                            TopBarActionMsg.ExpandDictMenu(
                                expand = false
                            )
                        )
                    },
                    onItemClick = { sendMessage(TopBarActionMsg.ChangeDict(lang = it)) },
                )
            }
        }
    )
}

@PreviewWidget
@Composable
fun AppBarWidgetPreview() {
    AppBarWidget(
        state = null,
        openAddDict = {},
        sendMessage = {},
    )
}
