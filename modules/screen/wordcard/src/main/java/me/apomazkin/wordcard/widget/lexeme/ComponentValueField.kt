package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.text.base.LexemeEditableText
import me.apomazkin.wordcard.mate.ComponentValueState

/**
 * Поле значения компонента (generic-замена LexemeMeaningField). Принимает готовый
 * `label` (резолв в LexemeComponentsBlock через componentValueLabel — A12).
 * P1: пустое значение при потере фокуса = удаление.
 */
@Composable
internal fun ComponentValueField(
    state: ComponentValueState,
    label: String,
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
            label = label,
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
