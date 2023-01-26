package me.apomazkin.langpicker.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        LazyColumn(
            modifier = Modifier
                .weight(1F),
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