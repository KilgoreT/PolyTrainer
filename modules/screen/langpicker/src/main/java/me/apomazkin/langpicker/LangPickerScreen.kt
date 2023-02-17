package me.apomazkin.langpicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.langpicker.entity.LangPresetUi
import me.apomazkin.langpicker.entity.LangUpdateUi
import me.apomazkin.langpicker.widget.LangPickerLoadingWidget
import me.apomazkin.langpicker.widget.LangPickerPresetWidget
import me.apomazkin.theme.AppTheme

@Composable
fun LangPickerScreen(
    langPickerUseCase: LangPickerUseCase,
    viewModel: LangPickerViewModel = viewModel(
        factory = LangPickerViewModel.Factory(langPickerUseCase)
    ),
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
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
        LangPickerState.LoadingState -> {
            LangPickerLoadingWidget()
        }
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
@Preview(
    showBackground = true,
    locale = "Ru"
)
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