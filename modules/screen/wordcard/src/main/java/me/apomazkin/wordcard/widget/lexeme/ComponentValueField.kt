package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.editUnderline
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.ui.text.base.LexemeEditableText
import me.apomazkin.wordcard.mate.ComponentValueState

/**
 * Поле значения компонента (generic-замена LexemeMeaningField). Принимает готовый
 * `label` (резолв в LexemeComponentsBlock через componentValueLabel — A12).
 * P1: пустое значение при потере фокуса = удаление.
 * IS486 (девайс-фидбек): CHOICE — ОДИН чип с названием выбранной опции (без
 * заголовка компонента и отдельной строки значения): тап по телу — пикер,
 * крестик — удаление значения.
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
    /** IS486 CHOICE: display выбранной опции (резолв в блоке). */
    selectedOptionLabel: String? = null,
    /** IS491 captioned: ввод в caption-поле открытого edit-режима. */
    onCaptionChange: (String) -> Unit = {},
    /** IS491 captioned: фокус в caption-поле — триггер загрузки подсказок. */
    onCaptionFocused: () -> Unit = {},
    /** IS491 captioned: подсказки компонента (из state, one-shot). */
    captionSuggestions: List<String> = emptyList(),
) {
    if (state.template == ComponentTemplate.CHOICE) {
        ChoiceValueChip(
            optionLabel = selectedOptionLabel.orEmpty(),
            enabled = enabled,
            onRemove = onRemove,
            modifier = modifier,
        )
        return
    }
    if (state.template == ComponentTemplate.CAPTIONED_TEXT) {
        CaptionedValueField(
            state = state,
            label = label,
            enabled = enabled,
            onValueChange = onValueChange,
            onOpenEditMode = onOpenEditMode,
            onCommitEdit = onCommitEdit,
            onRemove = onRemove,
            onCaptionChange = onCaptionChange,
            onCaptionFocused = onCaptionFocused,
            captionSuggestions = captionSuggestions,
            modifier = modifier,
        )
        return
    }
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
        if (state.isEdit) {
            HorizontalDivider(color = editUnderline)
        }
    }
}

/**
 * IS491: captioned_text-значение — многострочный text + caption-combobox (UC2–UC7).
 * View: text + подпись серым (если есть), тап — edit.
 *
 * Edit: оба поля в одной focus-группе; отдельные поля по потере фокуса НЕ коммитят.
 * Коммит (выход из edit) — когда фокус ушёл со ВСЕГО компонента, через дебаунс:
 * при передаче фокуса text↔caption есть кадр без фокуса на группе — ждём его и
 * коммитим только если фокус НЕ вернулся (ушли совсем). Иначе переключение полей
 * коммитило бы/удаляло значение или застревало бы в edit.
 */
@Composable
private fun CaptionedValueField(
    state: ComponentValueState,
    label: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCommitEdit: () -> Unit,
    onRemove: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onCaptionFocused: () -> Unit,
    captionSuggestions: List<String>,
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
        if (state.isEdit) {
            var hasFocus by remember { mutableStateOf(false) }
            var wasFocused by remember { mutableStateOf(false) }
            // Дебаунс коммита на уход фокуса: LaunchedEffect keyed на hasFocus —
            // если фокус вернулся в соседнее поле в пределах задержки, эффект
            // отменяется и коммита нет. Если остался снят — ушли со всего компонента.
            LaunchedEffect(hasFocus) {
                if (wasFocused && !hasFocus) {
                    delay(150)
                    if (!hasFocus) onCommitEdit()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        hasFocus = focusState.hasFocus
                        if (focusState.hasFocus) wasFocused = true
                    },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LexemeEditableText(
                    originValue = state.origin,
                    changedValue = state.edited,
                    isEditMode = true,
                    textColor = MaterialTheme.colorScheme.secondary,
                    textStyle = LexemeStyle.BodyL,
                    onTextChange = onValueChange,
                    onOpenEditMode = onOpenEditMode,
                    onFocusLost = { /* коммитит дебаунс группы, не поле */ },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(color = editUnderline)
                CaptionField(
                    caption = state.editedCaption.orEmpty(),
                    suggestions = captionSuggestions,
                    enabled = enabled,
                    onCaptionChange = onCaptionChange,
                    onFocused = onCaptionFocused,
                    onFocusLost = { /* коммитит дебаунс группы, не поле */ },
                )
            }
        } else {
            LexemeEditableText(
                originValue = state.origin,
                changedValue = state.edited,
                isEditMode = false,
                textColor = MaterialTheme.colorScheme.secondary,
                textStyle = LexemeStyle.BodyL,
                onTextChange = onValueChange,
                onOpenEditMode = onOpenEditMode,
                onFocusLost = { },
                modifier = Modifier.fillMaxWidth(),
            )
            state.originCaption?.let { caption ->
                Text(
                    modifier = Modifier.clickable(enabled = enabled, onClick = onOpenEditMode),
                    text = caption,
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                )
            }
        }
    }
}

/**
 * IS486: чип CHOICE-значения (облик [SubentityChip]). Тело ГЛУХОЕ (решение
 * 2026-07-21): смена опции = удалить крестиком + добавить заново через чип
 * добавления; пикер по тапу на значение не открывается.
 */
@Composable
private fun ChoiceValueChip(
    optionLabel: String,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier
                    .padding(PaddingValues(start = 12.dp, top = 5.dp, end = 4.dp, bottom = 5.dp)),
                text = optionLabel,
                style = LexemeStyle.BodyS,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Icon(
                modifier = Modifier
                    .clickable(enabled = enabled, onClick = onRemove)
                    .padding(PaddingValues(top = 5.dp, end = 8.dp, bottom = 5.dp))
                    .size(12.dp),
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
