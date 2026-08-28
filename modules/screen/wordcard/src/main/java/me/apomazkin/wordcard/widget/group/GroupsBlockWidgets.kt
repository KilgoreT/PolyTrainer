package me.apomazkin.wordcard.widget.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.mate.GroupUi
import me.apomazkin.wordcard.mate.GroupsBlockState
import me.apomazkin.wordcard.mate.Msg

/**
 * IS493 Э5 (D22.1): чипы групп слова в шапке карточки — ИНДИКАТОРЫ
 * (В4: тап открывает пикер). Намеренно отличимы от чипов компонентов
 * (ревью UX-5): без trailing-иконки действия; maxLines=1+ellipsis
 * (ревью UX-6). Пустой список — блока нет.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupChipsRow(
    chips: List<GroupUi>,
    enabled: Boolean,
    onChipClick: () -> Unit,
) {
    if (chips.isEmpty()) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            // Пара primary/onPrimary — прецедент SubentityChip (контраст
            // гарантирован темой); отличие от чипов компонентов — нет
            // trailing-иконки действия (ревью UX-5).
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clickable(enabled = enabled) { onChipClick() },
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    text = chip.name,
                    style = LexemeStyle.BodyM,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * IS493 Э5 (D22.1): пикер групп — M3 ModalBottomSheet, LazyColumn с
 * ограничением высоты (ревью UX-4: 20+ групп, landscape); строка =
 * имя + Checkbox; галочка in-flight дизейблится (keyed, В3); пустой
 * словарь групп — заглушка (создание из пикера — Э7).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupPickerBottomSheetWidget(
    block: GroupsBlockState,
    sendMessage: (Msg) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { sendMessage(Msg.DismissGroupPicker) },
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        // Фон — как у InputBottomSheetWidget (words/группы): без него
        // M3-дефолт тёмный и чёрный текст нечитаем (прогон M1).
        containerColor = MaterialTheme.colorScheme.onPrimary,
        dragHandle = {},
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(id = R.string.group_picker_title),
            style = LexemeStyle.H6,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (block.dictGroups.isEmpty()) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                text = stringResource(id = R.string.group_picker_empty),
                style = LexemeStyle.BodyM,
                color = grayTextColor,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(bottom = 16.dp),
            ) {
                items(items = block.dictGroups, key = { it.id }) { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = group.id !in block.inFlight) {
                                sendMessage(Msg.ToggleGroupMembership(groupId = group.id))
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = group.name,
                            style = LexemeStyle.BodyL,
                            // Явный цвет (прецедент GroupNodeWidget) — иначе
                            // contentColor шторки даёт нечитаемый серый.
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Checkbox(
                            checked = group.id in block.wordGroupIds,
                            enabled = group.id !in block.inFlight,
                            onCheckedChange = {
                                sendMessage(Msg.ToggleGroupMembership(groupId = group.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewChips() {
    AppTheme {
        GroupChipsRow(
            chips = listOf(GroupUi(1, "Дом"), GroupUi(2, "Быт")),
            enabled = true,
            onChipClick = {},
        )
    }
}
