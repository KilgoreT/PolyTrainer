package me.apomazkin.wordstab.ui.widget.photo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.dialogFieldBg
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.photo.PhotoCard
import me.apomazkin.wordstab.logic.photo.PhotoCardTarget
import me.apomazkin.wordstab.logic.photo.PhotoOwnGroup
import me.apomazkin.wordstab.logic.photo.PhotoSheet
import me.apomazkin.wordstab.logic.photo.PhotoTextField
import me.apomazkin.wordstab.logic.photo.existingOf
import me.apomazkin.wordstab.logic.photo.groupIdOr
import me.apomazkin.wordstab.logic.photo.targetsFor

/**
 * Нижние листы окна (набросок §2, §3, §5, §6): общая и своя группа, «куда
 * класть», часть речи, цель соответствия, правка текста. Один лист за
 * раз — какой открыт, хранит state. Цвета явные, парой из окна.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoSheets(
    state: PhotoWordsState,
    sendMessage: (Msg) -> Unit,
) {
    val sheet = state.sheet ?: return
    val form = state.form ?: return
    ModalBottomSheet(
        onDismissRequest = { sendMessage(Msg.ClosePhotoSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = formBackground,
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
        ) {
            when (sheet) {
                PhotoSheet.CommonGroup -> CommonGroupSheet(state = state, form = form, sendMessage = sendMessage)
                is PhotoSheet.CardGroup -> CardGroupSheet(lemma = sheet.lemma, state = state, form = form, sendMessage = sendMessage)
                is PhotoSheet.CardTarget -> CardTargetSheet(lemma = sheet.lemma, state = state, form = form, sendMessage = sendMessage)
                is PhotoSheet.PartOfSpeech -> PartOfSpeechSheet(sheet = sheet, state = state, form = form, sendMessage = sendMessage)
                is PhotoSheet.MappingTarget -> MappingTargetSheet(kind = sheet.kind, state = state, form = form, sendMessage = sendMessage)
                is PhotoSheet.TextEdit -> TextEditSheet(sheet = sheet, state = state, sendMessage = sendMessage)
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = LexemeStyle.BodyM,
        color = formTextTertiary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SheetHint(text: String) {
    Text(
        text = text,
        style = LexemeStyle.BodyS,
        color = formTextSecondary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** Строка выбора: маркер, текст акцентом у выбранного, подпись серым (прецедент LanguagePickerDialog). */
@Composable
private fun SheetRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = if (selected) "●" else "○",
            style = LexemeStyle.BodyM,
            color = if (selected) LexemeColor.primary else formTextSecondary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = text,
                style = LexemeStyle.BodyM,
                color = if (selected) LexemeColor.primary else formTextTertiary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = LexemeStyle.BodyS,
                    color = formTextSecondary,
                )
            }
        }
    }
}

/** Общая группа (AWP-08): «Все» или группа словаря; создать группу нельзя (AWP-08.3). */
@Composable
private fun CommonGroupSheet(
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    SheetTitle(text = stringResource(id = R.string.photo_add_common_group))
    SheetRow(
        text = stringResource(id = R.string.photo_add_group_all),
        selected = state.commonGroupId == null,
        onClick = { sendMessage(Msg.SetPhotoCommonGroup(null)) },
    )
    form.groups.forEach { group ->
        SheetRow(
            text = group.name,
            selected = state.commonGroupId == group.id,
            onClick = { sendMessage(Msg.SetPhotoCommonGroup(group.id)) },
        )
    }
    SheetHint(text = stringResource(id = R.string.photo_add_group_hint))
}

/** Своя группа карточки (AWP-08): как у всех, «Все» или группа. */
@Composable
private fun CardGroupSheet(
    lemma: String,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    val card = state.cards.firstOrNull { it.lemma == lemma } ?: return
    SheetTitle(text = stringResource(id = R.string.photo_add_card_group_title, lemma))
    SheetRow(
        text = stringResource(id = R.string.photo_add_group_as_common),
        selected = card.ownGroup == PhotoOwnGroup.AsCommon,
        onClick = { sendMessage(Msg.SetPhotoCardGroup(lemma, PhotoOwnGroup.AsCommon)) },
    )
    SheetRow(
        text = stringResource(id = R.string.photo_add_group_all),
        selected = card.ownGroup == PhotoOwnGroup.All,
        onClick = { sendMessage(Msg.SetPhotoCardGroup(lemma, PhotoOwnGroup.All)) },
    )
    form.groups.forEach { group ->
        SheetRow(
            text = group.name,
            selected = card.ownGroup == PhotoOwnGroup.Group(group.id),
            onClick = { sendMessage(Msg.SetPhotoCardGroup(lemma, PhotoOwnGroup.Group(group.id))) },
        )
    }
}

/** Куда класть (AWP-18): леммы словаря с этим текстом и «новое слово»; подсказка о группе (AWP-18.5). */
@Composable
private fun CardTargetSheet(
    lemma: String,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    val card = state.cards.firstOrNull { it.lemma == lemma } ?: return
    SheetTitle(text = stringResource(id = R.string.photo_add_target_title, lemma))
    form.existingOf(lemma).forEach { existing ->
        SheetRow(
            text = stringResource(
                id = R.string.photo_add_target_existing,
                existing.text,
                "${existing.coreValues.joinToString(", ")} · ${lexemesCount(existing.lexemeCount)}",
            ),
            selected = card.target == PhotoCardTarget.Existing(existing.wordId),
            onClick = { sendMessage(Msg.SetPhotoCardTarget(lemma, PhotoCardTarget.Existing(existing.wordId))) },
            subtitle = if (existing.groupNames.isEmpty()) {
                stringResource(id = R.string.photo_add_target_no_groups)
            } else {
                stringResource(id = R.string.photo_add_target_groups, existing.groupNames.joinToString(", "))
            },
        )
    }
    SheetRow(
        text = stringResource(id = R.string.photo_add_target_new, lemma),
        selected = card.target == PhotoCardTarget.NewWord,
        onClick = { sendMessage(Msg.SetPhotoCardTarget(lemma, PhotoCardTarget.NewWord)) },
    )
    GroupHint(card = card, state = state, form = form)
}

@Composable
private fun GroupHint(
    card: PhotoCard,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
) {
    val target = card.target as? PhotoCardTarget.Existing ?: return
    val groupId = card.groupIdOr(state.commonGroupId) ?: return
    val group = form.groups.firstOrNull { it.id == groupId } ?: return
    val existing = form.existingLemmas.firstOrNull { it.wordId == target.wordId } ?: return
    SheetHint(
        text = stringResource(
            id = R.string.photo_add_target_group_hint,
            group.name,
            "${existing.lexemeCount} ${stringResource(id = R.string.photo_add_old_lexemes)}",
        ),
    )
}

/** Часть речи лексемы-кандидата (AWP-17): опции словаря и «без части речи». */
@Composable
private fun PartOfSpeechSheet(
    sheet: PhotoSheet.PartOfSpeech,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    val lexeme = state.cards.firstOrNull { it.lemma == sheet.lemma }?.lexemes?.firstOrNull { it.key == sheet.key } ?: return
    SheetTitle(text = stringResource(id = R.string.photo_add_part_of_speech_title))
    form.partOfSpeechOptions.sortedBy { it.position }.forEach { option ->
        SheetRow(
            text = partOfSpeechLabel(option),
            selected = lexeme.partOfSpeechOptionId == option.id,
            onClick = { sendMessage(Msg.SetPhotoPartOfSpeech(sheet.lemma, sheet.key, option.id)) },
        )
    }
    SheetRow(
        text = stringResource(id = R.string.photo_add_part_of_speech_none),
        selected = lexeme.partOfSpeechOptionId == null,
        onClick = { sendMessage(Msg.SetPhotoPartOfSpeech(sheet.lemma, sheet.key, null)) },
    )
}

/** Цель вида данных (AWP-09): «не добавлять» и компоненты вида; занятый другим видом — с подписью. */
@Composable
private fun MappingTargetSheet(
    kind: PhotoDataKind,
    state: PhotoWordsState,
    form: PhotoDictionaryForm,
    sendMessage: (Msg) -> Unit,
) {
    val current = state.mapping.targetOf(kind)
    SheetTitle(text = stringResource(id = R.string.photo_add_mapping_target_title, kindLabel(kind, form.translationLanguage)))
    SheetRow(
        text = stringResource(id = R.string.photo_add_mapping_none),
        selected = current == PhotoTarget.None,
        onClick = { sendMessage(Msg.SetPhotoMapping(kind, PhotoTarget.None)) },
    )
    form.targetsFor(kind).forEach { component ->
        val target = PhotoTarget.Component(component.id)
        val takenBy = PhotoDataKind.entries.firstOrNull { it != kind && state.mapping.targetOf(it) == target }
        SheetRow(
            text = "«${componentLabel(component)}»",
            selected = current == target,
            onClick = { sendMessage(Msg.SetPhotoMapping(kind, target)) },
            subtitle = takenBy?.let { stringResource(id = R.string.photo_add_mapping_taken, kindLabel(it, form.translationLanguage).lowercase()) },
        )
    }
}

/** Правка текста (AWP-16): поле, «было: …» со «сбросить», отмена и сохранить. */
@Composable
private fun TextEditSheet(
    sheet: PhotoSheet.TextEdit,
    state: PhotoWordsState,
    sendMessage: (Msg) -> Unit,
) {
    val lexeme = state.cards.firstOrNull { it.lemma == sheet.lemma }?.lexemes?.firstOrNull { it.key == sheet.key } ?: return
    val original = when (val field = sheet.field) {
        PhotoTextField.Translation -> lexeme.translation.original
        PhotoTextField.Gloss -> lexeme.gloss?.original
        is PhotoTextField.Example -> lexeme.examples.getOrNull(field.index)?.original
    }
    val titleRes = when (sheet.field) {
        PhotoTextField.Translation -> R.string.photo_add_edit_translation
        PhotoTextField.Gloss -> R.string.photo_add_edit_gloss
        is PhotoTextField.Example -> R.string.photo_add_edit_example
    }
    SheetTitle(text = stringResource(id = titleRes))
    OutlinedTextField(
        value = sheet.draft,
        onValueChange = { sendMessage(Msg.UpdatePhotoDraft(it)) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = LexemeStyle.BodyM,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = formTextTertiary,
            unfocusedTextColor = formTextTertiary,
            focusedContainerColor = dialogFieldBg,
            unfocusedContainerColor = dialogFieldBg,
            focusedBorderColor = LexemeColor.primary,
            unfocusedBorderColor = cardDivider,
            cursorColor = LexemeColor.primary,
        ),
    )
    if (original != null && original != sheet.draft) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = R.string.photo_add_edit_was, original),
                style = LexemeStyle.BodyS,
                color = formTextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(id = R.string.photo_add_edit_reset),
                style = LexemeStyle.BodyS,
                color = LexemeColor.primary,
                modifier = Modifier
                    .clickable { sendMessage(Msg.ResetPhotoText(sheet.lemma, sheet.key, sheet.field)) }
                    .padding(start = 12.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CancelButtonWidget(modifier = Modifier.weight(1f)) { sendMessage(Msg.ClosePhotoSheet) }
        PrimaryFullButtonWidget(
            modifier = Modifier.weight(1f),
            titleRes = R.string.photo_add_edit_save,
            onClick = { sendMessage(Msg.SubmitPhotoText) },
        )
    }
}
