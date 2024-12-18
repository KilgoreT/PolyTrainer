package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.TextValueState

@Composable
fun LexemeItemWidget(
    order: Int,
    state: LexemeState,
    sendMessage: (Msg) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        LexemeTitleWidget(
            order = order,
            state = state,
            sendMessage = sendMessage
        )
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(12.dp),
            color = whiteColor,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                state.translation?.let { textValueState ->
                    LexemeValueFieldWidget(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp),
                        state = textValueState,
                        titleRes = R.string.word_card_bottom_translation,
                        onOpenEditMode = {
                            sendMessage(Msg.TranslationOpenEdit(lexemeId = state.id))
                        },
                        onTextChange = {
                            sendMessage(Msg.TranslationTextChange(lexemeId = state.id, value = it))
                        },
                        onCloseEditMode = {
                            sendMessage(Msg.TranslationEndEdit(lexemeId = state.id))
                        },
                        onActionIconClick = {
                            sendMessage(Msg.DeleteTranslation(lexemeId = state.id))
                        }
                    )
                }
                state.definition?.let {
                    state.translation?.let {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    LexemeValueFieldWidget(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp),
                        state = it,
                        titleRes = R.string.word_card_bottom_definition,
                        onOpenEditMode = {
                            sendMessage(Msg.DefinitionOpenEdit(lexemeId = state.id))
                        },
                        onTextChange = {
                            sendMessage(Msg.DefinitionTextChange(lexemeId = state.id, value = it))
                        },
                        onCloseEditMode = {
                            sendMessage(Msg.DefinitionEndEdit(lexemeId = state.id))
                        },
                        onActionIconClick = {
                            sendMessage(Msg.DeleteDefinition(lexemeId = state.id))
                        }
                    )
                }
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(24.dp),
        ) {
            LexemeItemWidget(
                order = 1,
                state = LexemeState(
                    definition = TextValueState(
                        origin = "a unit of meaning in a language, consisting of a word or group of words",
                        edited = "a unit of meaning in a language, consisting of a word or group of words",
                        isEdit = false,
                    ),
                    translation = TextValueState(
                        origin = "слово",
                        edited = "слово",
                        isEdit = false,
                    ),
                ),
                sendMessage = {},
            )
        }
    }
}