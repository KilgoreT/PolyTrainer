@file:OptIn(ExperimentalLifecycleComposeApi::class)

package me.apomazkin.langpicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.langpicker.logic.LangPickerState
import me.apomazkin.langpicker.logic.Msg
import me.apomazkin.langpicker.widget.LangLoadingWidget
import me.apomazkin.langpicker.widget.LangPickerWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.gradientSecondaryVertical
import me.apomazkin.ui.ImageBgGradWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun LangPickerScreen(
    langPickerUseCase: LangPickerUseCase,
    viewModel: LangPickerViewModel = viewModel(
        factory = LangPickerViewModel.Factory(langPickerUseCase)
    ),
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LangPickerScreen(
        state = state,
        onClose = onClose
    ) { viewModel.accept(it) }
}

@Composable
fun LangPickerScreen(
    state: LangPickerState,
    onClose: () -> Unit,
    sendMsg: (Msg) -> Unit,
) {
    ImageBgGradWidget(
        imageRes = R.drawable.ic_lang_pick_bg,
        brush = gradientSecondaryVertical
    ) {
        with(state) {
            if (needClose) {
                onClose.invoke()
            }
            if (isLoading) {
                LangLoadingWidget()
            } else {
                LangPickerWidget(
                    langState = langState,
                    sendMsg = sendMsg
                )
            }
        }
    }
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun Preview() {
    AppTheme {
        LangPickerScreen(
            state = LangPickerState(),
            onClose = {}
        ) {}
    }
}