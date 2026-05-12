package me.apomazkin.dictionary.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.di.viewModelFactory
import me.apomazkin.dictionary.form.widget.DictionaryFormWidget
import me.apomazkin.dictionary.widget.DictionaryAppBar
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictionaryFormScreen(
    factory: DictionaryFormViewModel.Factory,
    navigator: FormNavigator,
    editingDictionaryId: Long? = null,
    showAppBar: Boolean = true,
    viewModel: DictionaryFormViewModel = viewModel(
        key = "dictionaryForm_${editingDictionaryId ?: "new"}",
        factory = viewModelFactory { factory.create(editingDictionaryId, navigator) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DictionaryFormScreen(
        state = state,
        showAppBar = showAppBar,
    ) { viewModel.accept(it) }
}

@Composable
internal fun DictionaryFormScreen(
    state: DictionaryFormScreenState,
    showAppBar: Boolean = true,
    sendMsg: (DictionaryFormMsg) -> Unit,
) {
    BackHandler { sendMsg(DictionaryFormMsg.Back) }

    SystemBarsWidget(
        color = whiteColor,
    )
    Scaffold(
        topBar = {
            if (showAppBar) {
                DictionaryAppBar(onBackPress = { sendMsg(DictionaryFormMsg.Back) })
            }
        }
    ) { paddings ->
        Box(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .background(color = whiteColor)
        ) {
            DictionaryFormWidget(
                formState = state,
                sendMsg = sendMsg,
            )
        }
    }
}

@Composable
@PreviewWidget
private fun PreviewForm() {
    AppTheme {
        DictionaryFormScreen(
            state = DictionaryFormScreenState(),
        ) {}
    }
}
