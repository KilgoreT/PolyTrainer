package me.apomazkin.candidates.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidateValueUiItem
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.componentOf
import me.apomazkin.candidates.lexemeOf
import me.apomazkin.candidates.optionsOf
import me.apomazkin.candidates.targetRowsOf
import me.apomazkin.candidates.textOf
import me.apomazkin.candidates.valueOf
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.dialogFieldBg
import me.apomazkin.theme.formBackground
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
import java.text.DateFormat
import java.util.Date

/**
 * Нижние листы окна и диалога: общая и своя группа (AWP-08), «куда класть»
 * (AWP-18), опция любого «Выбора» (AWP-17), правка текста (AWP-16). Один
 * лист за раз — какой открыт, хранит стейт. Цвета явные, парой из окна.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidatesSheets(
    view: CandidatesView,
    onAction: (CandidateAction) -> Unit,
) {
    val sheet = view.sheet ?: return
    val form = view.form ?: return
    ModalBottomSheet(
        onDismissRequest = { onAction(CandidateAction.CloseSheet) },
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
                CandidatesSheetUi.CommonGroup ->
                    CommonGroupSheet(view, form, onAction)
                is CandidatesSheetUi.CardGroup ->
                    CardGroupSheet(sheet.lemma, sheet.key, view, form, onAction)
                is CandidatesSheetUi.CardTarget ->
                    CardTargetSheet(sheet.lemma, sheet.key, view, form, onAction)
                is CandidatesSheetUi.Option ->
                    OptionSheet(sheet.ref, view, form, onAction)
                is CandidatesSheetUi.TextEdit ->
                    TextEditSheet(sheet, view, form, onAction)
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

/**
 * Строка выбора: маркер, текст акцентом у выбранного, подпись серым (прецедент
 * LanguagePickerDialog).
 */
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
    view: CandidatesView,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    SheetTitle(text = stringResource(id = R.string.photo_add_common_group))
    SheetRow(
        text = stringResource(id = R.string.photo_add_group_all),
        selected = view.commonGroupId == null,
        onClick = { onAction(CandidateAction.SetCommonGroup(null)) },
    )
    form.groups.forEach { group ->
        SheetRow(
            text = group.name,
            selected = view.commonGroupId == group.id,
            onClick = { onAction(CandidateAction.SetCommonGroup(group.id)) },
        )
    }
    SheetHint(text = stringResource(id = R.string.photo_add_group_hint))
}

/** Своя группа плашки при «новое слово» (AWP-08): как у всех, «Все» или группа. */
@Composable
private fun CardGroupSheet(
    lemma: String,
    key: String,
    view: CandidatesView,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    val lexeme = view.cards.lexemeOf(lemma, key) ?: return
    SheetTitle(text = stringResource(id = R.string.photo_add_card_group_title, lemma))
    SheetRow(
        text = stringResource(id = R.string.photo_add_group_as_common),
        selected = lexeme.ownGroup == CardOwnGroupUi.AsCommon,
        onClick = { onAction(CandidateAction.SetCardGroup(lemma, key, CardOwnGroupUi.AsCommon)) },
    )
    SheetRow(
        text = stringResource(id = R.string.photo_add_group_all),
        selected = lexeme.ownGroup == CardOwnGroupUi.All,
        onClick = { onAction(CandidateAction.SetCardGroup(lemma, key, CardOwnGroupUi.All)) },
    )
    form.groups.forEach { group ->
        SheetRow(
            text = group.name,
            selected = lexeme.ownGroup == CardOwnGroupUi.Group(group.id),
            onClick = {
                onAction(CandidateAction.SetCardGroup(lemma, key, CardOwnGroupUi.Group(group.id)))
            },
        )
    }
}

/**
 * «Куда» плашки (AWP-18): по строке на слово словаря с этим текстом —
 * значения ядра, число лексем, группы или «Все», день добавления в коротком
 * формате локали — и «новое слово». Лист только рисует строки
 * `targetRowsOf`.
 */
@Composable
private fun CardTargetSheet(
    lemma: String,
    key: String,
    view: CandidatesView,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    val lexeme = view.cards.lexemeOf(lemma, key) ?: return
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    SheetTitle(text = stringResource(id = R.string.photo_add_target_title, lemma))
    form.targetRowsOf(lemma).forEach { row ->
        val groups = if (row.groupNames.isEmpty()) {
            stringResource(id = R.string.photo_add_target_no_groups)
        } else {
            row.groupNames.joinToString(", ")
        }
        SheetRow(
            text = stringResource(
                id = R.string.photo_add_target_existing,
                row.coreValues.joinToString(", "),
                lexemesCount(row.lexemeCount),
                groups,
                dateFormat.format(Date(row.addedAt)),
            ),
            selected = lexeme.target == CardTargetUi.Existing(row.wordId),
            onClick = {
                val target = CardTargetUi.Existing(row.wordId)
                onAction(CandidateAction.SetCardTarget(lemma, key, target))
            },
        )
    }
    SheetRow(
        text = stringResource(id = R.string.photo_add_target_new, lemma),
        selected = lexeme.target == CardTargetUi.NewWord,
        onClick = { onAction(CandidateAction.SetCardTarget(lemma, key, CardTargetUi.NewWord)) },
    )
}

/**
 * Опция «Выбора» у лексемы-кандидата (AWP-17): опции этого компонента и
 * «без значения». Отмечены выбранные; у «несколько» нажатие добавляет или
 * снимает опцию, решает редьюсер.
 */
@Composable
private fun OptionSheet(
    ref: ComponentRefUi,
    view: CandidatesView,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    val component = form.componentOf(ref.componentId) ?: return
    val value = view.cards.valueOf(ref) as? CandidateValueUiItem.Choice ?: return
    SheetTitle(text = componentLabel(component))
    form.optionsOf(ref.componentId).sortedBy { it.position }.forEach { option ->
        SheetRow(
            text = optionLabel(option),
            selected = option.id in value.options,
            onClick = { onAction(CandidateAction.SetOption(ref, option.id)) },
        )
    }
    SheetRow(
        text = stringResource(id = R.string.candidates_option_none),
        selected = value.options.isEmpty(),
        onClick = { onAction(CandidateAction.SetOption(ref, null)) },
    )
}

/**
 * Правка текста по адресу значения (AWP-16): поле, «было: …» со
 * «сбросить» — только по флагу [CandidatesSheetUi.TextEdit.canReset], UI
 * тексты не сравнивает (AWP-16.6); «Сохранить» шлёт адрес и текст из листа.
 */
@Composable
private fun TextEditSheet(
    sheet: CandidatesSheetUi.TextEdit,
    view: CandidatesView,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    SheetTitle(text = componentLabel(form, sheet.ref.componentId))
    OutlinedTextField(
        value = sheet.draft,
        onValueChange = { onAction(CandidateAction.UpdateDraft(it)) },
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
    val original = view.cards.textOf(sheet.ref)?.original
    if (sheet.canReset && original != null) {
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
                    .clickable { onAction(CandidateAction.ResetText(sheet.ref)) }
                    .padding(start = 12.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CancelButtonWidget(modifier = Modifier.weight(1f)) { onAction(CandidateAction.CloseSheet) }
        PrimaryFullButtonWidget(
            modifier = Modifier.weight(1f),
            titleRes = R.string.photo_add_edit_save,
            onClick = { onAction(CandidateAction.SubmitText(sheet.ref, sheet.draft)) },
        )
    }
}
