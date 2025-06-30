@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.dictionaryappbar

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionaryappbar.mate.DictionaryAppBarState
import me.apomazkin.dictionaryappbar.mate.Msg
import me.apomazkin.dictionaryappbar.widget.AppBarTitleWidget
import me.apomazkin.dictionarypicker.DictDropDownWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictionaryAppBar(
        @StringRes titleResId: Int,
        logger: LexemeLogger,
        dictionaryAppBarUseCase: DictionaryAppBarUseCase,
        openAddDict: () -> Unit,
        viewModel: DictionaryAppBarViewModel = viewModel(
                factory = DictionaryAppBarViewModel.Factory(
                        logger = logger,
                        useCase = dictionaryAppBarUseCase,
                )
        ),
) {
    val state: DictionaryAppBarState by viewModel.state.collectAsStateWithLifecycle()
    DictionaryAppBar(
            titleResId = titleResId,
            state = state,
            openAddDict = openAddDict,
    ) { viewModel.accept(it) }
}

@Composable
internal fun DictionaryAppBar(
        @StringRes titleResId: Int,
        state: DictionaryAppBarState,
        openAddDict: () -> Unit,
        sendMessage: (Msg) -> Unit,
) {

    TopAppBar(
            title = { AppBarTitleWidget(titleResId = titleResId) },
            actions = {
                if (state.isLoading) {
                    CircularProgressIndicator(
                            modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(24.dp)
                            ,
                            color = LexemeColor.primary,
                            strokeWidth = 4.dp,
                            trackColor = Color.Transparent,
                    )
                } else {
                    DictDropDownWidget(
                            dictList = state.availableDictList,
                            currentDict = state.currentDict,
                            isExpand = state.isDropDownMenuOpen,
                            openAddDict = openAddDict,
                            onOpenDropDown = { sendMessage(Msg.DictMenuOn) },
                            onDismiss = { sendMessage(Msg.DictMenuOff) },
                            onItemClick = { sendMessage(Msg.ChangeDict(dict = it)) },
                    )
                }
            }
    )

}

@PreviewWidget
@Composable
private fun PreviewWidget() {
    AppTheme {
        DictionaryAppBar(
                titleResId = R.string.quiz_tab_title,
                state = DictionaryAppBarState(),
                openAddDict = {},
                sendMessage = {},
        )
    }
}