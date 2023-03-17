package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.M3Black
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.TopBarActionMsg
import me.apomazkin.vocabulary.logic.TopBarActionState
import me.apomazkin.vocabulary.tools.DataHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWidget(
    state: TopBarActionState,
    onAddLang: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    TopAppBar(
        title = {
            Text(text = stringResource(id = R.string.item_title_vocabulary))
        },
        actions = {
            if (!state.isLoading) {
                LangDropDownWidget(
                    iconRes = state.currentLang?.iconRes ?: throw IllegalStateException(""),
                    langList = state.availableLangList,
                    isExpand = state.isDropDownMenuOpen,
                    onChangeLang = { sendMessage(TopBarActionMsg.ChangeLang(numericCode = it)) },
                    onAddLang = onAddLang,
                    sendMessage = sendMessage,
                )
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = M3Black,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun Preview() {
    AppTheme {
        TopBarWidget(
            state = DataHelper.State.loaded.topBarActionState,
            onAddLang = {},
        ) {}
    }
}