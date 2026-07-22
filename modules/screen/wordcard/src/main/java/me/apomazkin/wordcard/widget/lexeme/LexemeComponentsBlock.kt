package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg

/**
 * Оркестратор лексемы: список значений (ComponentValueField) + ряд chip'ов для добавления.
 * IS486: чипы — по правилу участия (addableTypeIds); CHOICE — пикер-диалог опций,
 * выбор коммитит сразу (Msg.SelectComponentOption). Открытие диалога — локальный UI-state.
 */
@Composable
internal fun LexemeComponentsBlock(
    lexemeState: LexemeState,
    availableTypes: List<ComponentType>,
    optionsByType: Map<ComponentTypeId, List<ComponentOption>>,
    addableTypeIds: Set<ComponentTypeId>,
    enabled: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    // Локальный UI-state пикера: typeId открытого CHOICE (null = закрыт).
    var pickerTypeId by remember { mutableStateOf<ComponentTypeId?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (lexemeState.components.isEmpty()) {
            Text(
                text = stringResource(id = R.string.word_card_draft_hint),
                style = LexemeStyle.BodyM,
                color = formTextSecondary,
            )
        }
        lexemeState.components.forEach { cv ->
            key(cv.key) {
                val selectedOptionLabel = cv.selectedOptionId?.let { optionId ->
                    optionsByType[cv.componentTypeId]
                        ?.firstOrNull { it.id == optionId }
                        ?.let { optionDisplayLabel(it) }
                }
                ComponentValueField(
                    state = cv,
                    label = componentValueLabel(cv.componentTypeId, cv.componentTypeRef, availableTypes),
                    enabled = enabled,
                    onValueChange = { sendMessage(Msg.UpdateComponentValueInput(lexemeState.id, cv.key, it)) },
                    onOpenEditMode = { sendMessage(Msg.EnterComponentValueEditMode(lexemeState.id, cv.key)) },
                    onCommitEdit = { sendMessage(Msg.CommitComponentValueEdit(lexemeState.id, cv.key)) },
                    onRemove = { sendMessage(Msg.RemoveComponentValueRequested(lexemeState.id, cv.key)) },
                    selectedOptionLabel = selectedOptionLabel,
                )
            }
        }
        ComponentChipsRow(
            availableTypes = availableTypes,
            addableTypeIds = addableTypeIds,
            enabled = enabled,
            onAddComponent = { typeId -> sendMessage(Msg.CreateComponentValue(lexemeState.id, typeId)) },
            onAddChoice = { type -> pickerTypeId = type.id },
        )
    }

    pickerTypeId?.let { typeId ->
        val type = availableTypes.firstOrNull { it.id == typeId }
        val options = optionsByType[typeId].orEmpty()
        if (type != null && options.isNotEmpty()) {
            // Решение 2026-07-21: пикер — только добавление (смена = удалить +
            // добавить), преселекта текущей опции не существует.
            OptionPickerDialog(
                title = componentLabelOf(type),
                options = options,
                selectedOptionId = null,
                onSelect = { optionId ->
                    sendMessage(Msg.SelectComponentOption(lexemeState.id, typeId, optionId))
                },
                onDismiss = { pickerTypeId = null },
            )
        } else {
            // Guard пустого пикера (спека, следствие «пустой CHOICE валиден»): нечего выбирать.
            pickerTypeId = null
        }
    }
}
