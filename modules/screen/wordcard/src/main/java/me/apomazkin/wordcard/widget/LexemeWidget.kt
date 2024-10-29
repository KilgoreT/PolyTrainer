package me.apomazkin.wordcard.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.chippicker.ChipPickerWidget
import me.apomazkin.chippicker.lexicalCategory
import me.apomazkin.chippicker.toChipPicker
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R
import me.apomazkin.wordcard.mate.DefinitionState
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.toValue

@Composable
fun LexemeWidget(
    state: LexemeState,
    sendMessage: (Msg) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (state.isEdit) {
                    OutlinedIconButton(
                        onClick = { sendMessage(Msg.ResetLexeme(lexemeId = state.id)) }
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "")
                    }
                    OutlinedIconButton(
                        onClick = { sendMessage(Msg.SaveLexeme(lexemeId = state.id)) }
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = "")
                    }
                } else {
                    OutlinedIconButton(
                        onClick = { sendMessage(Msg.DeleteLexeme(lexemeId = state.id)) }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "")
                    }
                    OutlinedIconButton(
                        onClick = { sendMessage(Msg.EditLexeme(lexemeId = state.id)) }
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "")
                    }
                }
            }
            ChipPickerWidget(
                title = stringResource(id = R.string.vocabulary_detail_speech_part),
                pickerValue = state.category.toValue(state.isEdit).toChipPicker(),
                chipList = lexicalCategory,
                onChipSelect = { sendMessage(Msg.LexicalCategoryChange(state.id, it)) },
                onResetChip = { sendMessage(Msg.LexicalCategoryReset(state.id)) },
                editable = state.isEdit
            )
            Spacer(modifier = Modifier.height(16.dp))
            EditableText(
                title = stringResource(id = R.string.vocabulary_detail_definition_title),
                isEdit = state.isEdit,
                value = state.definition.toValue(state.isEdit),
                onTextChange = { sendMessage(Msg.DefinitionChange(state.id, it)) }
            )
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        LexemeWidget(
            state = LexemeState(
                isEdit = false,
                id = 0,
                definition = DefinitionState("a unit of meaning in a language, consisting of a word or group of words")
            ),
            sendMessage = {}
        )
    }
}