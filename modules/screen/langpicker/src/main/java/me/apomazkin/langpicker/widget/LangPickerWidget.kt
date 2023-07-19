package me.apomazkin.langpicker.widget

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.apomazkin.langpicker.R
import me.apomazkin.langpicker.logic.LangState
import me.apomazkin.langpicker.logic.Msg
import me.apomazkin.langpicker.toLangNameRes
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.btn.PrimaryButtonWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun BoxScope.LangPickerWidget(
    langState: LangState,
    sendMsg: (Msg) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
    ) {
        LangListWidget(
            langList = langState.langList,
            selectedNumericCode = langState.selectedNumericCode,
            sendMsg = sendMsg,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButtonWidget(
            titleRes = R.string.lang_selection_button,
            enabled = langState.addLangButtonEnable
        ) {
            langState.selectedNumericCode?.let { numericCode ->
                val langName = context.getString(numericCode.toLangNameRes())
                sendMsg(Msg.SaveLang(numericCode, langName))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        Box {
            LangPickerWidget(langState = LangState()) {}
        }
    }
}