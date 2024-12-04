@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package me.apomazkin.wordcard.widget.addlexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.dividerColor
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R
import me.apomazkin.wordcard.mate.AddLexemeBottomState
import me.apomazkin.wordcard.mate.Msg

private const val PADDING = 16
private const val CORNER_RADIUS = 28

@Composable
internal fun AddLexemeBottomWidget(
    state: AddLexemeBottomState,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismiss: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    ModalBottomSheet(
        modifier = Modifier,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.onPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = dividerColor) },
        shape = RoundedCornerShape(topStart = CORNER_RADIUS.dp, topEnd = CORNER_RADIUS.dp),
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PADDING.dp)
                .padding(bottom = PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.word_card_bottom_title),
                style = LexemeStyle.H6,
                color = MaterialTheme.colorScheme.secondary,
            )
            LexemeMeaningWidget(
                titleRes = R.string.word_card_bottom_translation,
                isChecked = state.isTranslationCheck,
            ) { sendMessage(Msg.AddLexemeBottomTranslation(isAdded = it)) }
            LexemeMeaningWidget(
                titleRes = R.string.word_card_bottom_definition,
                isChecked = state.isDefinitionCheck,
            ) { sendMessage(Msg.AddLexemeBottomDefinition(isAdded = it)) }
            ActionsWidget(
                actionButtonEnabled = state.isTranslationCheck || state.isDefinitionCheck,
                onDismiss = onDismiss,
                onConfirm = { sendMessage(Msg.AddLexeme) },
            )
        }
    }
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        AddLexemeBottomWidget(
            state = AddLexemeBottomState(),
            sheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Expanded
            ),
            onDismiss = {},
        ) {}
    }
}