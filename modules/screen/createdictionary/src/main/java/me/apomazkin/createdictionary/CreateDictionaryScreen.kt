package me.apomazkin.createdictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.createdictionary.logic.CreateDictionaryState
import me.apomazkin.createdictionary.logic.LangState
import me.apomazkin.createdictionary.logic.Msg
import me.apomazkin.createdictionary.widget.LangPickerWidget
import me.apomazkin.createdictionary.widget.LoadingWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.White
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun CreateDictionaryScreen(
    createDictionaryUseCase: CreateDictionaryUseCase,
    viewModel: CreateDictionaryViewModel = viewModel(
        factory = CreateDictionaryViewModel.Factory(createDictionaryUseCase)
    ),
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CreateDictionaryScreen(
        state = state,
        onClose = onClose
    ) { viewModel.accept(it) }
}

@Composable
fun CreateDictionaryScreen(
    state: CreateDictionaryState,
    onClose: () -> Unit,
    sendMsg: (Msg) -> Unit,
) {
    SystemBarsWidget(
        color = White,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(
                color = White
            )
    ) {
        if (state.needClose) {
            onClose.invoke()
        }
        if (state.isLoading) {
            LoadingWidget()
        } else {
            LangPickerWidget(
                langState = state.langState,
                sendMsg = sendMsg
            )
        }
    }
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun Preview() {
    AppTheme {
        CreateDictionaryScreen(
            state = CreateDictionaryState(
                isLoading = false,
                langState = LangState(
                    langList = LanguageData.langPreviewList,
                )
            ),
            onClose = {}
        ) {}
    }
}