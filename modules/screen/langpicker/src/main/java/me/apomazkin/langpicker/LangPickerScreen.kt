package me.apomazkin.langpicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.langpicker.entity.LangPresetUi
import me.apomazkin.langpicker.entity.LangUpdateUi
import me.apomazkin.langpicker.widget.LangPickerPresetWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewScreenEn
import me.apomazkin.ui.preview.PreviewScreenRu

@OptIn(ExperimentalLifecycleComposeApi::class)
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
    ) {
        viewModel.setupLang(it)
    }
}

@Composable
fun LangPickerScreen(
    state: LangPickerState,
    onClose: () -> Unit,
    onSelectLang: (lang: LangUpdateUi) -> Unit,
) {
    when (state) {
        LangPickerState.LoadingState -> {}
        is LangPickerState.PresetState -> {
            LaunchedEffect(state) {
                if (state.isSelected) {
                    onClose.invoke()
                }
            }
            LangPickerPresetWidget(
                state = state,
                onSelectLang = onSelectLang
            )
        }
    }
}

@Composable
@PreviewScreenRu
@PreviewScreenEn
private fun Preview() {
    AppTheme {
        LangPickerScreen(
            state = LangPickerState.PresetState(
                listOf(
                    LangPresetUi(
                        flagRes = R.drawable.ic_more_on_primary,
                        countryNumericCode = 826,
                        langNameRes = R.string.lang_english,
                    ),
                    LangPresetUi(
                        flagRes = R.drawable.ic_more_on_primary,
                        countryNumericCode = 250,
                        langNameRes = R.string.lang_italian,
                    ),
                )
            ),
            onClose = {}
        ) {}
    }
}