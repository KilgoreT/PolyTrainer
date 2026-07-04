package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg

/**
 * Оркестратор лексемы: список значений (ComponentValueField) + ряд chip'ов для добавления.
 */
@Composable
internal fun LexemeComponentsBlock(
    lexemeState: LexemeState,
    availableTypes: List<ComponentType>,
    enabled: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lexemeState.components.forEach { cv ->
            key(cv.key) {
                ComponentValueField(
                    state = cv,
                    label = componentValueLabel(cv.componentTypeId, cv.componentTypeRef, availableTypes),
                    enabled = enabled,
                    onValueChange = { sendMessage(Msg.UpdateComponentValueInput(lexemeState.id, cv.key, it)) },
                    onOpenEditMode = { sendMessage(Msg.EnterComponentValueEditMode(lexemeState.id, cv.key)) },
                    onCommitEdit = { sendMessage(Msg.CommitComponentValueEdit(lexemeState.id, cv.key)) },
                    onRemove = { sendMessage(Msg.RemoveComponentValueRequested(lexemeState.id, cv.key)) },
                )
            }
        }
        ComponentChipsRow(
            availableTypes = availableTypes,
            addedNonMultipleTypeIds = lexemeState.addedNonMultipleTypeIds,
            enabled = enabled,
            onAddComponent = { typeId -> sendMessage(Msg.CreateComponentValue(lexemeState.id, typeId)) },
        )
    }
}
