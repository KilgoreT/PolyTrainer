package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.radioBorderInactive
import me.apomazkin.theme.radioSelectedBg
import me.apomazkin.theme.templateChipText
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.dialog.base.LexemeDialog

/**
 * IS486 фаза 2: пикер значения CHOICE-компонента (решение 2026-07-19, вариант 3):
 * диалог с радио-рядами в визуальном языке IS485 (`ComponentRadioRow` конструктора —
 * локальная копия: те же токены темы, internal-виджеты component_widgets недоступны).
 * Выбор коммитит сразу (Msg.SelectComponentOption) — edit-режима нет.
 */
@Composable
internal fun OptionPickerDialog(
    title: String,
    options: List<ComponentOption>,
    selectedOptionId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    LexemeDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title,
                style = LexemeStyle.H6,
                color = blackColor,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    OptionRadioRow(
                        text = optionDisplayLabel(option),
                        selected = option.id == selectedOptionId,
                        onClick = {
                            onSelect(option.id)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

/** Радио-ряд опции — визуальный язык IS485 (стиль ComponentRadioRow конструктора). */
@Composable
private fun OptionRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) radioSelectedBg else whiteColor,
        border = BorderStroke(1.dp, if (selected) LexemeColor.primary else radioBorderInactive),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (selected) LexemeColor.primary else radioBorderInactive,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = LexemeColor.primary, shape = CircleShape),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = if (selected) LexemeStyle.BodyMBold else LexemeStyle.BodyM,
                color = if (selected) LexemeColor.secondary else templateChipText,
            )
        }
    }
}
