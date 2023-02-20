package me.apomazkin.langpicker.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.langpicker.LangPickerState
import me.apomazkin.langpicker.R
import me.apomazkin.langpicker.entity.LangUpdateUi
import me.apomazkin.ui.ImageRoundedWidget
import me.apomazkin.ui.StatusBarColorWidget

@Composable
fun LangPickerPresetWidget(
    state: LangPickerState.PresetState,
    onSelectLang: (lang: LangUpdateUi) -> Unit,
) {

    val selectedLang = remember { mutableStateOf<LangUpdateUi?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessageState: State<String?> = rememberUpdatedState(
        newValue = state.errorMessage?.message?.let {
            stringResource(id = it)
        }
    )
    LaunchedEffect(state) {
        if (state.errorMessage != null) {
            errorMessageState.value?.let {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    StatusBarColorWidget()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        ImageRoundedWidget(
            imageRes = R.drawable.image_lang_selection,
            bottomStart = 18,
            bottomEnd = 18,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            LazyColumn(
                modifier = Modifier
                    .matchParentSize()
            ) {
                titleItemWidget()
                languagesPreset(
                    list = state.value,
                    selected = selectedLang.value?.langName
                ) {
                    selectedLang.value = it
                }
                listSubtitleWidget { }
            }
            SnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                hostState = snackbarHostState
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ContinueButtonWidget(
            isEnable = selectedLang.value != null
        ) {
            selectedLang.value?.let {
                onSelectLang.invoke(it)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}