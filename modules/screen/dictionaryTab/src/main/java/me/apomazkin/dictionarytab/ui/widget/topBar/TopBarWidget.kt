package me.apomazkin.dictionarytab.ui.widget.topBar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.dictionarypicker.state.LangPickerState
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.logic.TopBarActionMsg
import me.apomazkin.dictionarytab.tools.DataHelper
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWidget(
    state: LangPickerState?,
    openAddDict: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.item_title_vocabulary),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        actions = {
            state?.let { langState ->
                me.apomazkin.dictionarypicker.DictDropDownWidget(
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
        },
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        TopBarWidget(
            state = DataHelper.State.loaded.topBarState.langPickerState,
            openAddDict = {},
        ) {}
    }
}