package me.apomazkin.wordstab.ui.widget.photo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.photo.PhotoSheet

/**
 * Шапка окна (набросок §2): общая группа (AWP-08) и соответствия (AWP-09)
 * — сводка с именами компонентов, «изменить» раскрывает четыре строки;
 * сводка красная, если ни один вид данных не идёт в ядро (AWP-11.7).
 */
@Composable
internal fun PhotoHeaderWidget(
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.CommonGroup)) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.photo_add_common_group),
                style = LexemeStyle.BodyM,
                color = formTextTertiary,
            )
            Text(
                text = "${commonGroupLabel(state, form)} ▾",
                style = LexemeStyle.BodyM,
                color = LexemeColor.primary,
            )
        }
        HorizontalDivider(color = cardDivider)
        if (state.isMappingExpanded) {
            MappingExpanded(state = state, form = form, sendMessage = sendMessage)
        } else {
            MappingSummary(state = state, form = form, sendMessage = sendMessage)
        }
    }
}

@Composable
private fun commonGroupLabel(
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
): String = form.groups.firstOrNull { it.id == state.commonGroupId }?.name ?: stringResource(id = R.string.photo_add_group_all)

@Composable
private fun MappingSummary(
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    // map — inline, внутри можно звать composable; joinToString с лямбдой — нет.
    val parts = PhotoDataKind.entries.map { kind ->
        "${kindLabel(kind, form.translationLanguage).lowercase()} → ${targetLabel(state.mapping.targetOf(kind), form)}"
    }
    val summary = parts.joinToString(", ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { sendMessage(Msg.TogglePhotoMapping) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.photo_add_mapping_title),
                style = LexemeStyle.BodyM,
                color = formTextTertiary,
            )
            Text(
                text = summary,
                style = LexemeStyle.BodyS,
                color = if (state.summary.noCoreMapping) destructiveRed else formTextSecondary,
            )
        }
        Text(
            text = stringResource(id = R.string.photo_add_mapping_change),
            style = LexemeStyle.BodyS,
            color = LexemeColor.primary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun MappingExpanded(
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sendMessage(Msg.TogglePhotoMapping) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.photo_add_mapping_title),
                style = LexemeStyle.BodyM,
                color = if (state.summary.noCoreMapping) destructiveRed else formTextTertiary,
            )
            Text(
                text = stringResource(id = R.string.photo_add_mapping_collapse),
                style = LexemeStyle.BodyS,
                color = LexemeColor.primary,
            )
        }
        PhotoDataKind.entries.forEach { kind ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sendMessage(Msg.OpenPhotoSheet(PhotoSheet.MappingTarget(kind))) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = kindLabel(kind, form.translationLanguage),
                    style = LexemeStyle.BodyM,
                    color = formTextTertiary,
                )
                Text(
                    text = "${targetLabel(state.mapping.targetOf(kind), form)} ▾",
                    style = LexemeStyle.BodyM,
                    color = LexemeColor.primary,
                )
            }
        }
        Text(
            text = stringResource(id = R.string.photo_add_mapping_remembered),
            style = LexemeStyle.BodyS,
            color = formTextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}
