package me.apomazkin.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.dictionary.logic.DictionaryState
import me.apomazkin.dictionary.logic.DictionarySelectionState
import me.apomazkin.dictionary.logic.Msg
import me.apomazkin.dictionary.widget.DictionaryAppBar
import me.apomazkin.dictionary.widget.DictionaryPickerWidget
import me.apomazkin.dictionary.widget.LoadingWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictionaryScreen(
    dictionaryUseCase: DictionaryUseCase,
    viewModel: DictionaryViewModel = viewModel(
        factory = DictionaryViewModel.Factory(dictionaryUseCase)
    ),
    onClose: () -> Unit,
    onBackPress: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DictionaryScreen(
        state = state,
        onClose = onClose,
        onBackPress = onBackPress,
    ) { viewModel.accept(it) }
}

@Composable
internal fun DictionaryScreen(
    state: DictionaryState,
    onClose: () -> Unit,
    onBackPress: (() -> Unit)? = null,
    sendMsg: (Msg) -> Unit,
) {
    SystemBarsWidget(
        color = whiteColor,
    )
    Scaffold(
        topBar = {
            onBackPress?.let { DictionaryAppBar(onBackPress = it) }
        }
    ) { paddings ->
        Box(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .background(
                    color = whiteColor
                )
        ) {
            if (state.needClose) {
                onClose.invoke()
            }
            if (state.isLoading) {
                LoadingWidget()
            } else {
                DictionaryPickerWidget(
                    dictionarySelectionState = state.dictionarySelectionState,
                    sendMsg = sendMsg
                )
            }
        }
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        DictionaryScreen(
            state = DictionaryState(
                isLoading = false,
                dictionarySelectionState = DictionarySelectionState(
                    dictionaryList = DictionaryData.dictionaryPreviewList,
                )
            ),
            onClose = {}
        ) {}
    }
}
