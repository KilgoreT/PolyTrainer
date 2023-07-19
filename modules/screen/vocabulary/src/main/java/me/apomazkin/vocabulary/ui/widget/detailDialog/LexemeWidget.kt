package me.apomazkin.vocabulary.ui.widget.detailDialog

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.chippicker.ChipPickerWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.entity.LexemeLabel
import me.apomazkin.vocabulary.entity.lexicalCategory
import me.apomazkin.vocabulary.entity.toChipPicker
import me.apomazkin.vocabulary.logic.LexemeState
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.WordDetailMsg

@Composable
fun LexemeWidget(
    state: LexemeState,
    sendMsg: (Msg) -> Unit,
) {
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = Color.Black,
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(top = 16.dp)
            .padding(horizontal = 24.dp),
    ) {
        if (state.requireSave) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
            ) {
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { }
                ) {
                    Text(text = "Отменить")
                }
                OutlinedButton(
                    modifier = Modifier,
                    onClick = { }
                ) {
                    Text(text = "Сохранить")
                }
            }
        } else {
            Icon(
                modifier = Modifier
                    .align(End),
                imageVector = Icons.Default.Edit, contentDescription = ""
            )
        }
        ChipPickerWidget(
            title = stringResource(id = R.string.vocabulary_detail_speech_part),
            pickerValue = state.category.toChipPicker(),
            chipList = lexicalCategory,
            onChipSelect = { sendMsg(WordDetailMsg.LexicalCategory(state.lexemeId, it)) },
            onResetChip = { sendMsg(WordDetailMsg.ResetLexemeCategory(state.lexemeId)) },
            editable = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        WordDetailTextField(
            state = state.definition,
            title = stringResource(id = R.string.vocabulary_detail_definition_title),
            onEditStart = { sendMsg(WordDetailMsg.DefinitionEditStart(state.lexemeId)) },
            onTextChange = { sendMsg(WordDetailMsg.DefinitionUpdate(state.lexemeId, it)) },
            onEditFinish = { sendMsg(WordDetailMsg.DefinitionEditFinish(state.lexemeId, it)) }
        )
//        Spacer(modifier = Modifier.height(16.dp))
//        WordDetailTextField(
//            state = EditableTextState(),
//            title = stringResource(id = R.string.vocabulary_detail_translation_title),
//            onEditStart = {},
//            onTextChange = { sendMsg(WordDetailMsg.TranslationUpdate(state.lexemeId, it)) },
//            onEditFinish = {}
//        )
        state.lexemeId?.let {
            OutlinedButton(
                onClick = {
//                    sendMsg(Msg.DeleteLexeme(lexemeId = it))
                }
            ) {
                Text(text = "Удалить перевод")
            }
        }
    }
}

@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        LexemeWidget(
            state = LexemeState(
                lexemeId = null,
                requireSave = false,
                category = LexemeLabel.PHRASE,
            )
        ) {}
    }
}

@PreviewWidgetEn
@Composable
private fun PreviewEdit() {
    AppTheme {
        LexemeWidget(
            state = LexemeState(
                lexemeId = null,
                requireSave = true,
            )
        ) {}
    }
}