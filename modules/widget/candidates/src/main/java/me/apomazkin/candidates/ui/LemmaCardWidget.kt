package me.apomazkin.candidates.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidateValueUiItem
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardLexemeUiItem
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.FormComponentUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.SavedLexemeUi
import me.apomazkin.candidates.SavedWordUi
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.componentOf
import me.apomazkin.candidates.existingOf
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.cardDivider
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.theme.formTextTertiary
import java.text.DateFormat
import java.util.Date

/**
 * Плашка словаря под словом окна снимка (AWP-46): группы слова или «Все» и
 * день добавления коротким форматом локали одной строкой, под ней
 * сохранённые лексемы — значения строками «Имя: значение», без крестиков и
 * правки; слово без лексем — одна строка групп и даты (AWP-46.4).
 */
@Composable
fun SavedWordPlate(
    saved: SavedWordUi,
    form: DictionaryFormUi,
) {
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val groups = if (saved.groupNames.isEmpty()) {
        stringResource(id = R.string.photo_add_target_no_groups)
    } else {
        saved.groupNames.joinToString(", ")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
    ) {
        Text(
            text = "$groups · ${dateFormat.format(Date(saved.addedAt))}",
            style = LexemeStyle.BodyS,
            color = LexemeColor.primary,
        )
        saved.lexemes.forEachIndexed { index, lexeme ->
            if (index > 0) {
                HorizontalDivider(color = cardDivider, modifier = Modifier.padding(vertical = 4.dp))
            }
            SavedLexemeLines(lexeme = lexeme, form = form)
        }
    }
}

/** Значения сохранённой лексемы строками «Имя: значение» по компонентам формы. */
@Composable
private fun SavedLexemeLines(
    lexeme: SavedLexemeUi,
    form: DictionaryFormUi,
) {
    lexeme.values.forEach { value ->
        val component = form.componentOf(value.componentId) ?: return@forEach
        val name = componentLabel(component)
        when (value) {
            is CandidateValueUiItem.Text -> value.texts.forEach { item ->
                ValueLine(name = name, value = item.text, valueColor = formTextTertiary)
            }
            is CandidateValueUiItem.Choice -> value.options.forEach { optionId ->
                ValueLine(name = name, value = optionLabel(form, optionId), valueColor = formTextTertiary)
            }
        }
    }
}

/**
 * «Куда: …▾» (AWP-18) и «Группа: …▾» (AWP-08) у плашки нейронки: «Куда» —
 * когда у слова есть леммы словаря (AWP-18.2), «Группа» — при «новое
 * слово» (AWP-08.6); без групп в словаре «Группа» без стрелки (AWP-08.3).
 */
@Composable
fun LexemeChoices(
    card: LemmaCardUiItem,
    lexeme: CardLexemeUiItem,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    val hasExisting = form.existingOf(card.lemma).isNotEmpty()
    val isNewWord = lexeme.target == CardTargetUi.NewWord
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (hasExisting) {
            Choice(
                label = stringResource(id = R.string.photo_add_where),
                value = targetName(lexeme.target, form),
                enabled = true,
                onClick = {
                    val sheet = CandidatesSheetUi.CardTarget(card.lemma, lexeme.key)
                    onAction(CandidateAction.OpenSheet(sheet))
                },
            )
        }
        if (isNewWord) {
            Choice(
                label = stringResource(id = R.string.photo_add_group),
                value = ownGroupName(lexeme.ownGroup, form),
                enabled = form.groups.isNotEmpty(),
                onClick = {
                    val sheet = CandidatesSheetUi.CardGroup(card.lemma, lexeme.key)
                    onAction(CandidateAction.OpenSheet(sheet))
                },
            )
        }
    }
}

@Composable
private fun Choice(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.clickable(enabled = enabled) { onClick() }) {
        Text(text = label, style = LexemeStyle.BodyS, color = formTextSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (enabled) "$value ▾" else value,
            style = LexemeStyle.BodyS,
            color = if (enabled) LexemeColor.primary else formTextTertiary,
        )
    }
}

@Composable
private fun targetName(
    target: CardTargetUi,
    form: DictionaryFormUi,
): String {
    val chosen = target as? CardTargetUi.Existing
    val existing = form.existingLemmas.firstOrNull { it.wordId == chosen?.wordId }
    return if (existing == null) {
        stringResource(id = R.string.photo_add_new_word)
    } else {
        existingName(existing)
    }
}

/** «run» или «run» (бегать): первое значение ядра помогает отличить одноимённые слова. */
private fun existingName(existing: ExistingLemmaUi): String {
    val first = existing.coreValues.firstOrNull() ?: return "«${existing.text}»"
    return "«${existing.text}» ($first)"
}

@Composable
private fun ownGroupName(
    own: CardOwnGroupUi,
    form: DictionaryFormUi,
): String = when (own) {
    CardOwnGroupUi.AsCommon -> stringResource(id = R.string.photo_add_group_as_common)
    CardOwnGroupUi.All -> stringResource(id = R.string.photo_add_group_all)
    is CardOwnGroupUi.Group -> form.groups.firstOrNull { it.id == own.groupId }?.name
        ?: stringResource(id = R.string.photo_add_group_as_common)
}

/**
 * Строка лексемы-кандидата (AWP-29): пометки «в тексте», «англ.»,
 * «изменено», «уже в словаре» или «есть», «не попадёт в ядро» и крестик
 * лексемы по [onDelete] — null, крестика нет: у плашки окна снимка своя
 * кнопка «Не нужно» (AWP-64); под ними активные значения по компонентам в
 * порядке значений лексемы. Текст компонента — «Имя: текст», нажатие
 * правит; у «Выбора» — «Имя: подпись ▾», нажатие открывает лист опций; у
 * компонента с несколькими значениями у каждого свой крестик.
 */
@Composable
fun LexemeRow(
    card: LemmaCardUiItem,
    lexeme: CardLexemeUiItem,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
    @StringRes existsMarkRes: Int = R.string.photo_add_exists_mark,
    onDelete: (() -> Unit)? = { onAction(CandidateAction.DeleteLexeme(card.lemma, lexeme.key)) },
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Marks(card = card, lexeme = lexeme, existsMarkRes = existsMarkRes)
            Spacer(modifier = Modifier.weight(1f))
            if (onDelete != null) {
                Cross(onClick = onDelete)
            }
        }
        lexeme.values.filter { it.isActive }.forEach { value ->
            val component = form.componentOf(value.componentId) ?: return@forEach
            when (value) {
                is CandidateValueUiItem.Text -> TextLines(
                    card = card,
                    lexeme = lexeme,
                    value = value,
                    component = component,
                    onAction = onAction,
                )
                is CandidateValueUiItem.Choice -> ChoiceLines(
                    card = card,
                    lexeme = lexeme,
                    value = value,
                    component = component,
                    form = form,
                    onAction = onAction,
                )
            }
        }
    }
}

/** Тексты компонента по строке на текст; нажатие открывает лист правки по адресу (AWP-16). */
@Composable
private fun TextLines(
    card: LemmaCardUiItem,
    lexeme: CardLexemeUiItem,
    value: CandidateValueUiItem.Text,
    component: FormComponentUi,
    onAction: (CandidateAction) -> Unit,
) {
    val name = componentLabel(component)
    value.texts.forEachIndexed { index, item ->
        val ref = ValueRefUi(card.lemma, lexeme.key, value.componentId, index)
        val sheet = CandidatesSheetUi.TextEdit(ref = ref, draft = item.text, canReset = false)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ValueLine(
                name = name,
                value = item.text,
                valueColor = formTextTertiary,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAction(CandidateAction.OpenSheet(sheet)) },
            )
            if (component.isMultiple) {
                Cross(onClick = { onAction(CandidateAction.DeleteValue(ref)) })
            }
        }
    }
}

/**
 * Опции «Выбора» по строке на опцию, пусто — «без значения»; нажатие открывает
 * лист опций (AWP-17).
 */
@Composable
private fun ChoiceLines(
    card: LemmaCardUiItem,
    lexeme: CardLexemeUiItem,
    value: CandidateValueUiItem.Choice,
    component: FormComponentUi,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    val name = componentLabel(component)
    val componentRef = ComponentRefUi(card.lemma, lexeme.key, value.componentId)
    val open = { onAction(CandidateAction.OpenSheet(CandidatesSheetUi.Option(componentRef))) }
    if (value.options.isEmpty()) {
        ValueLine(
            name = name,
            value = stringResource(id = R.string.candidates_option_none) + " ▾",
            valueColor = LexemeColor.primary,
            modifier = Modifier.clickable { open() },
        )
        return
    }
    value.options.forEachIndexed { index, optionId ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ValueLine(
                name = name,
                value = optionLabel(form, optionId) + " ▾",
                valueColor = LexemeColor.primary,
                modifier = Modifier
                    .weight(1f)
                    .clickable { open() },
            )
            if (component.isMultiple) {
                val ref = ValueRefUi(card.lemma, lexeme.key, value.componentId, index)
                Cross(onClick = { onAction(CandidateAction.DeleteValue(ref)) })
            }
        }
    }
}

/** «Имя: значение»: имя компонента вторым планом, значение своим цветом. */
@Composable
private fun ValueLine(
    name: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = formTextSecondary)) { append("$name: ") }
            withStyle(SpanStyle(color = valueColor)) { append(value) }
        },
        style = LexemeStyle.BodyM,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

/**
 * Пометки лексемы-кандидата: «в тексте» (AWP-43), «англ.» (AWP-34),
 * «изменено», «уже в словаре» или «есть» ([existsMarkRes]), «есть в «…»»
 * (WCL-15.4), «не попадёт в ядро» (AWP-11).
 */
@Composable
private fun Marks(
    card: LemmaCardUiItem,
    lexeme: CardLexemeUiItem,
    @StringRes existsMarkRes: Int,
) {
    val marks = buildList {
        if (lexeme.isInContext) {
            add(stringResource(id = R.string.scan_in_context) to LexemeColor.primary)
        }
        if (lexeme.isEnglishFallback) {
            add(stringResource(id = R.string.photo_add_english_mark) to formTextSecondary)
        }
        if (lexeme.isEdited) {
            add(stringResource(id = R.string.photo_add_edited_mark) to formTextSecondary)
        }
        if (lexeme.exists) {
            add(stringResource(id = existsMarkRes) to LexemeColor.primary)
        }
        // «Есть в «correr»» — только когда это не «есть» у самого слова диалога.
        if (lexeme.existsInLemma && !lexeme.exists) {
            val text = stringResource(id = R.string.candidates_exists_in_lemma, card.lemma)
            add(text to LexemeColor.primary)
        }
        if (lexeme.lacksCore) {
            add(stringResource(id = R.string.photo_add_no_core_mark) to destructiveRed)
        }
    }
    marks.forEach { (text, color) ->
        Text(
            text = text,
            style = LexemeStyle.BodyS,
            color = color,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
internal fun Cross(onClick: () -> Unit) {
    Text(
        text = "×",
        style = LexemeStyle.BodyXL,
        color = formTextSecondary,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}
