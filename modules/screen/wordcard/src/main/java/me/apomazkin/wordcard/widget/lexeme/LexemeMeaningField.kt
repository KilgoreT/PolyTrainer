package me.apomazkin.wordcard.widget.lexeme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.ui.text.base.LexemeEditableText
import me.apomazkin.wordcard.mate.TextValueState

/**
 * Chip-заголовок субсущности и inline-editable поле значения.
 *
 * @param labelRes подпись chip-заголовка.
 * @param state значение поля (origin/edited/isEdit).
 * @param enabled блокирует ввод и клик ✕.
 * @param onValueChange изменение текста.
 * @param onOpenEditMode тап по значению в view-mode.
 * @param onCommitEdit потеря фокуса с непустым значением.
 * @param onRemove тап ✕ или потеря фокуса с пустым значением.
 */
@Composable
internal fun LexemeMeaningField(
    @StringRes labelRes: Int,
    state: TextValueState,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCommitEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SubentityChip(
            labelRes = labelRes,
            iconRes = R.drawable.ic_close,
            enabled = enabled,
            onClick = onRemove,
        )
        LexemeEditableText(
            originValue = state.origin,
            changedValue = state.edited,
            isEditMode = state.isEdit,
            textColor = MaterialTheme.colorScheme.secondary,
            textStyle = LexemeStyle.BodyL,
            onTextChange = onValueChange,
            onOpenEditMode = onOpenEditMode,
            onFocusLost = { value ->
                if (value.isEmpty()) onRemove() else onCommitEdit()
            },
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewView() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            LexemeMeaningField(
                labelRes = R.string.word_card_bottom_translation,
                state = TextValueState(
                    origin = "слово",
                    edited = "слово",
                    isEdit = false,
                ),
                enabled = true,
                onValueChange = {},
                onOpenEditMode = {},
                onCommitEdit = {},
                onRemove = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewEdit() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            LexemeMeaningField(
                labelRes = R.string.word_card_bottom_definition,
                state = TextValueState(
                    origin = "",
                    edited = "новое значение",
                    isEdit = true,
                ),
                enabled = true,
                onValueChange = {},
                onOpenEditMode = {},
                onCommitEdit = {},
                onRemove = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewDisabled() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(whiteColor)
                .padding(16.dp),
        ) {
            LexemeMeaningField(
                labelRes = R.string.word_card_bottom_translation,
                state = TextValueState(
                    origin = "слово",
                    edited = "слово",
                    isEdit = false,
                ),
                enabled = false,
                onValueChange = {},
                onOpenEditMode = {},
                onCommitEdit = {},
                onRemove = {},
            )
        }
    }
}
