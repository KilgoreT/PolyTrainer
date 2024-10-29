package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.TopBarState
import me.apomazkin.vocabulary.tools.DataHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWidget(
    state: TopBarState.Main,
    openAddDict: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.item_title_vocabulary),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        },
        actions = {
            if (!state.isLoading) {
                DictDropDownWidget(
                    iconRes = state.currentDict?.flagRes ?: throw IllegalStateException(""),
                    currentDictCode = state.currentDict.numericCode,
                    dictList = state.availableDictList,
                    isExpand = state.isDropDownMenuOpen,
                    openAddDict = openAddDict,
                    sendMessage = sendMessage,
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
            state = DataHelper.State.loaded.topBarState.mainState,
            openAddDict = {},
        ) {}
    }
}