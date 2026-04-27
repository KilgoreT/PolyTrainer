package me.apomazkin.dictionary.form

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
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.dictionary.form.widget.DictionaryFormWidget
import me.apomazkin.dictionary.form.widget.LanguagePickerBottomSheet
import me.apomazkin.dictionary.widget.DictionaryAppBar
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictionaryFormScreen(
    dictionaryUseCase: DictionaryUseCase,
    editingDictionaryId: Long? = null,
    editingName: String = "",
    editingHasFlag: Boolean = false,
    viewModel: DictionaryFormViewModel = viewModel(
        factory = DictionaryFormViewModel.Factory(
            dictionaryUseCase,
            editingDictionaryId,
            editingName,
            editingHasFlag,
        )
    ),
    onClose: () -> Unit,
    onBackPress: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DictionaryFormScreen(
        state = state,
        onClose = onClose,
        onBackPress = onBackPress,
    ) { viewModel.accept(it) }
}

@Composable
internal fun DictionaryFormScreen(
    state: DictionaryFormScreenState,
    onClose: () -> Unit,
    onBackPress: (() -> Unit)? = null,
    sendMsg: (DictionaryFormMsg) -> Unit,
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
                .background(color = whiteColor)
        ) {
            if (state.needClose) {
                onClose.invoke()
            }
            DictionaryFormWidget(
                formState = state,
                sendMsg = sendMsg,
            )
        }
    }

    if (state.languagePickerState.show) {
        LanguagePickerBottomSheet(
            state = state.languagePickerState,
            onQueryChange = { sendMsg(DictionaryFormMsg.LanguageQueryChanged(it)) },
            onSelect = { sendMsg(DictionaryFormMsg.SelectLanguage(it)) },
            onDismiss = { sendMsg(DictionaryFormMsg.CloseLanguagePicker) },
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewForm() {
    AppTheme {
        DictionaryFormScreen(
            state = DictionaryFormScreenState(),
            onClose = {},
            onBackPress = {},
        ) {}
    }
}
