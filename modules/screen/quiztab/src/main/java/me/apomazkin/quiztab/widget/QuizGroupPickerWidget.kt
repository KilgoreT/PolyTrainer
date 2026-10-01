package me.apomazkin.quiztab.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.apomazkin.quiztab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Пункт пикера групп; `groupId == null` — «Все».
 * Непригодные пункты видимы, но некликабельны — порог самообъясняющий.
 */
data class QuizGroupPickerItem(
    val groupId: Long?,
    val title: String,
    val wordCount: Int,
    val isEligible: Boolean,
)

private const val MENU_MAX_WIDTH_FRACTION = 0.9f
private const val DROPDOWN_MAX_HEIGHT = 400

/**
 * Пикер набора групп карточки квиза: контрол «<имя> +N ▾» прижат к
 * правому краю, зона клика — вся строка на полную ширину, минимум 48dp
 * (промах мимо текста не улетает в открытие чата). Обрезается только
 * имя: «+N» и шеврон видны всегда.
 *
 * Меню — от правого края, ширина по самому длинному пункту, но не
 * больше 90% карточки. У каждого пункта, включая «Все», галка-индикатор
 * (клик обрабатывает только строка). Клик по группе переключает её и
 * оставляет меню открытым — можно отметить несколько; клик по «Все»
 * выбирает весь словарь и закрывает меню.
 *
 * @param selectedName имя первой выбранной группы либо «Все».
 * @param moreCount сколько групп выбрано кроме первой; 0 — «+N» нет.
 * @param selectedGroupIds выбранные группы; пусто — отмечено «Все».
 */
@Composable
fun QuizGroupPickerWidget(
    modifier: Modifier = Modifier,
    selectedName: String,
    moreCount: Int,
    selectedGroupIds: Set<Long>,
    items: List<QuizGroupPickerItem>,
    enabled: Boolean,
    onPickAll: () -> Unit,
    onToggle: (groupId: Long, checked: Boolean) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val pickerCd = stringResource(R.string.quiz_group_picker_cd)
    val controlColor = if (enabled) MaterialTheme.colorScheme.secondary else grayTextColor
    val controlStyle = LexemeStyle.BodyM.copy(color = controlColor)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val menuMaxWidth = maxWidth * MENU_MAX_WIDTH_FRACTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(enabled = enabled) { isExpanded = true }
                .semantics { contentDescription = pickerCd },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f, fill = false),
                text = selectedName,
                style = controlStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // «+N» и шеврон — вне ellipsis-текста: при длинном имени
            // набор не выглядит одной группой, индикатор не уезжает.
            if (moreCount > 0) {
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = stringResource(R.string.quiz_group_label_more, moreCount),
                    style = controlStyle,
                    maxLines = 1,
                )
            }
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = "▾",
                style = controlStyle,
            )
        }
        // Нулевой якорь у правого края: меню раскрывается от него и
        // само сдвигается, чтобы не вылезти за экран.
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            DropdownMenu(
                // Явный фон вместо M3-дефолта (чёрный контейнер) —
                // прецедент CaptionField/iconDropDowned.
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .widthIn(max = menuMaxWidth)
                    .heightIn(max = DROPDOWN_MAX_HEIGHT.dp),
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                items.forEach { item ->
                    val groupId = item.groupId
                    val isChecked = if (groupId == null) {
                        selectedGroupIds.isEmpty()
                    } else {
                        groupId in selectedGroupIds
                    }
                    DropdownMenuItem(
                        enabled = item.isEligible,
                        leadingIcon = {
                            // Индикатор: клик ловит строка, у галки своего
                            // обработчика нет — одна цель касания.
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                enabled = item.isEligible,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.quiz_group_option_count,
                                    item.title,
                                    item.wordCount,
                                ),
                                style = LexemeStyle.BodyM.copy(
                                    color = if (item.isEligible) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        grayTextColor
                                    },
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            if (groupId == null) {
                                isExpanded = false
                                onPickAll()
                            } else {
                                onToggle(groupId, !isChecked)
                            }
                        },
                    )
                }
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() = AppTheme {
    Column(modifier = Modifier.padding(16.dp)) {
        QuizGroupPickerWidget(
            selectedName = "Очень длинное название группы",
            moreCount = 2,
            selectedGroupIds = setOf(1L, 3L, 4L),
            items = listOf(
                QuizGroupPickerItem(groupId = null, title = "Все", wordCount = 47, isEligible = true),
                QuizGroupPickerItem(groupId = 1L, title = "Быт", wordCount = 12, isEligible = true),
                QuizGroupPickerItem(groupId = 2L, title = "Кухня", wordCount = 2, isEligible = false),
            ),
            enabled = true,
            onPickAll = {},
            onToggle = { _, _ -> },
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewDisabled() = AppTheme {
    Column(modifier = Modifier.padding(16.dp)) {
        QuizGroupPickerWidget(
            selectedName = "Все",
            moreCount = 0,
            selectedGroupIds = emptySet(),
            items = emptyList(),
            enabled = false,
            onPickAll = {},
            onToggle = { _, _ -> },
        )
    }
}
