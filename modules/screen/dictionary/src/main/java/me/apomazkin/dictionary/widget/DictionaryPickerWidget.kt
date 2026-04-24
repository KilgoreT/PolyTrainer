package me.apomazkin.dictionary.widget

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
import me.apomazkin.dictionary.DictionaryData
import me.apomazkin.dictionary.R
import me.apomazkin.dictionary.logic.DictionarySelectionState
import me.apomazkin.dictionary.logic.Msg
import me.apomazkin.dictionary.toDictionaryNameRes
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.ui.preview.PreviewWidget

private const val BOTTOM_PADDING = 16

@Composable
fun BoxScope.DictionaryPickerWidget(
    dictionarySelectionState: DictionarySelectionState,
    sendMsg: (Msg) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp),
    ) {
        DictionaryListWidget(
            dictionaryList = dictionarySelectionState.dictionaryList,
            selectedNumericCode = dictionarySelectionState.selectedNumericCode,
            sendMsg = sendMsg,
        )
        Spacer(modifier = Modifier.weight(1F))

        PrimaryFullButtonWidget(
            titleRes = R.string.dictionary_selection_button,
            enabled = dictionarySelectionState.addDictionaryButtonEnable
        ) {
            dictionarySelectionState.selectedNumericCode?.let { numericCode ->
                val dictionaryName = context.getString(numericCode.toDictionaryNameRes())
                sendMsg(Msg.SaveDictionary(numericCode, dictionaryName))
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
            DictionaryPickerWidget(
                dictionarySelectionState = DictionarySelectionState(
                    dictionaryList = DictionaryData.dictionaryPreviewList,
                    addDictionaryButtonEnable = true,
                )
            ) {}
        }
    }
}
