package me.apomazkin.createdictionary.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.apomazkin.createdictionary.LanguageData
import me.apomazkin.createdictionary.R
import me.apomazkin.createdictionary.logic.LangState
import me.apomazkin.createdictionary.logic.Msg
import me.apomazkin.createdictionary.toLangNameRes
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.btn.PrimaryButtonWidget
import me.apomazkin.ui.preview.PreviewWidget

private const val BOTTOM_PADDING = 16

@Composable
fun BoxScope.LangPickerWidget(
    langState: LangState,
    sendMsg: (Msg) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp),
    ) {
        LangListWidget(
            langList = langState.langList,
            selectedNumericCode = langState.selectedNumericCode,
            sendMsg = sendMsg,
        )
        Spacer(modifier = Modifier.weight(1F))

        PrimaryButtonWidget(
            titleRes = R.string.lang_selection_button,
            enabled = langState.addLangButtonEnable
        ) {
            langState.selectedNumericCode?.let { numericCode ->
                val langName = context.getString(numericCode.toLangNameRes())
                sendMsg(Msg.SaveLang(numericCode, langName))
            }
        }
        Spacer(modifier = Modifier.height(BOTTOM_PADDING.dp))
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(whiteColor)
        ) {
            LangPickerWidget(
                langState = LangState(
                    langList = LanguageData.langPreviewList,
                    addLangButtonEnable = true,
                )
            ) {}
        }
    }
}