package me.apomazkin.grouptree

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.dividerColor
import me.apomazkin.theme.grayTextColor
import me.apomazkin.theme.groupNodeBgColor
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Строка-заголовок узла дерева групп (IS493/Э2, D8.3).
 *
 * ТОЛЬКО строка: имя + счётчик + шеврон. Content-слота НЕТ намеренно —
 * содержимое раскрытого узла (строки слов, футер) обязано быть items()
 * LazyColumn потребителя, иначе чанк склеивается в один item и
 * виртуализация умирает. «Аккордеон» собирается на уровне LazyListScope
 * в экране-потребителе.
 *
 * Дерево/отступы вложенности — Э4.
 */
@Composable
fun GroupNodeWidget(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    // IS493 Э3 (D15.3): trailing-слот действий (kebab); null — нет
    // (у «Все» — всегда null, А7). Вложенный clickable потребляет тап —
    // onToggle строки не триггерится.
    actions: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        // Лёгкий голубой фон — визуально отличает узел группы от карточки
        // слова (решение юзера, ручная проверка Э2).
        color = groupNodeBgColor,
        border = BorderStroke(width = 1.dp, color = dividerColor),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = LexemeStyle.BodyXLBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = count.toString(),
                style = LexemeStyle.BodyM,
                color = grayTextColor,
            )
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
                tint = grayTextColor,
            )
            actions?.invoke()
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewCollapsed() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GroupNodeWidget(
                title = "Все",
                count = 12,
                isExpanded = false,
                onToggle = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewExpanded() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GroupNodeWidget(
                title = "Все",
                count = 128,
                isExpanded = true,
                onToggle = {},
            )
        }
    }
}
