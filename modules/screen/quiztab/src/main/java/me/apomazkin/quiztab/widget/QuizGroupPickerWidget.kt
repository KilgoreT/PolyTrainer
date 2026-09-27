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
 * IS500. Пункт пикера группы; `groupId == null` — «Все».
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
 * IS500. Пикер группы карточки квиза: контрол «<имя> ▾» прижат к
 * правому краю, зона клика — вся строка на полную ширину, минимум
 * 48dp (промах мимо текста не улетает в открытие чата). Выпадающий
 * список раскрывается от правого края, ширина — по самому длинному
 * пункту, но не больше 90% ширины карточки.
 */
@Composable
fun QuizGroupPickerWidget(
    modifier: Modifier = Modifier,
    selectedTitle: String,
    selectedGroupId: Long?,
    items: List<QuizGroupPickerItem>,
    enabled: Boolean,
    onPick: (Long?) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val pickerCd = stringResource(R.string.quiz_group_picker_cd)
    val controlColor = if (enabled) MaterialTheme.colorScheme.secondary else grayTextColor
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
                text = selectedTitle,
                style = LexemeStyle.BodyM.copy(color = controlColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Шеврон вне ellipsis-текста: индикатор раскрытия не
            // уезжает за край при длинном имени группы.
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = "▾",
                style = LexemeStyle.BodyM.copy(color = controlColor),
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
                    val isSelected = item.groupId == selectedGroupId
                    DropdownMenuItem(
                        enabled = item.isEligible,
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.quiz_group_option_count,
                                    item.title,
                                    item.wordCount,
                                ),
                                style = if (isSelected) {
                                    LexemeStyle.BodyMBold.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                } else {
                                    LexemeStyle.BodyM.copy(
                                        color = if (item.isEligible) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            grayTextColor
                                        },
                                    )
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            isExpanded = false
                            onPick(item.groupId)
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
            selectedTitle = "Все",
            selectedGroupId = null,
            items = listOf(
                QuizGroupPickerItem(groupId = null, title = "Все", wordCount = 47, isEligible = true),
                QuizGroupPickerItem(groupId = 1L, title = "Быт", wordCount = 12, isEligible = true),
                QuizGroupPickerItem(groupId = 2L, title = "Кухня", wordCount = 2, isEligible = false),
            ),
            enabled = true,
            onPick = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewDisabled() = AppTheme {
    Column(modifier = Modifier.padding(16.dp)) {
        QuizGroupPickerWidget(
            selectedTitle = "Все",
            selectedGroupId = null,
            items = emptyList(),
            enabled = false,
            onPick = {},
        )
    }
}
