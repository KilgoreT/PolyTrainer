package me.apomazkin.candidates.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Карточка леммы под словом (AWP-42, AWP-43), всегда целиком: лемма с
 * пометкой «есть в словаре», слово текста, «куда класть» (AWP-18), своя
 * группа (AWP-08) и лексемы-кандидаты со значениями по компонентам
 * (AWP-29). Крестик карточки — по [onDeleteCard], null — крестика нет: у
 * окна снимка слово удаляет своя кнопка (AWP-48); текст пометки «уже в
 * словаре» или «есть» — [existsMarkRes]. Свёрнутого вида нет.
 */
@Composable
fun LemmaCardWidget(
    card: LemmaCardUiItem,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
    onDeleteCard: (() -> Unit)?,
    @StringRes existsMarkRes: Int = R.string.photo_add_exists_mark,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CardHead(card = card, form = form, onDeleteCard = onDeleteCard)
        CardChoices(card = card, form = form, onAction = onAction)
        card.lexemes.forEach { lexeme ->
            HorizontalDivider(color = cardDivider, modifier = Modifier.padding(start = 16.dp))
            LexemeRow(
                card = card,
                lexeme = lexeme,
                form = form,
                onAction = onAction,
                existsMarkRes = existsMarkRes,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Сохранённые лексемы леммы словаря под заголовком «В словаре» (AWP-46):
 * значения как у лексемы-кандидата, без крестиков и правки.
 */
@Composable
fun SavedLexemeRows(
    saved: List<SavedLexemeUi>,
    form: DictionaryFormUi,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.scan_saved_title),
            style = LexemeStyle.BodyS,
            color = formTextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        saved.forEach { lexeme ->
            HorizontalDivider(color = cardDivider, modifier = Modifier.padding(start = 16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
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
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** Лемма с пометкой «есть в словаре», слова текста и своя группа, крестик карточки, если есть кому его слушать. */
@Composable
private fun CardHead(
    card: LemmaCardUiItem,
    form: DictionaryFormUi,
    onDeleteCard: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = card.lemma,
                style = LexemeStyle.BodyXLBold,
                color = formTextTertiary,
                modifier = Modifier.weight(1f),
            )
            if (card.existingCount > 0) {
                Text(
                    text = if (card.existingCount == 1) {
                        stringResource(id = R.string.photo_add_exists)
                    } else {
                        stringResource(id = R.string.photo_add_exists_count, card.existingCount)
                    },
                    style = LexemeStyle.BodyS,
                    color = LexemeColor.primary,
                )
            }
            if (onDeleteCard != null) {
                Cross(onClick = onDeleteCard)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    id = R.string.photo_add_in_text,
                    card.sourceWords.joinToString(", "),
                ),
                style = LexemeStyle.BodyS,
                color = formTextSecondary,
            )
            val own = card.ownGroup
            if (own is CardOwnGroupUi.Group) {
                val groupName = form.groups.firstOrNull { it.id == own.groupId }?.name.orEmpty()
                Text(
                    text = "⌂ $groupName",
                    style = LexemeStyle.BodyS,
                    color = LexemeColor.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}

/** «Куда: …▾» (AWP-18) и «Группа: …▾» (AWP-08) у карточки. */
@Composable
private fun CardChoices(
    card: LemmaCardUiItem,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
) {
    val hasExisting = form.existingOf(card.lemma).isNotEmpty()
    val hasGroups = form.groups.isNotEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Choice(
            label = stringResource(id = R.string.photo_add_where),
            value = targetName(card, form),
            enabled = hasExisting,
            onClick = {
                onAction(CandidateAction.OpenSheet(CandidatesSheetUi.CardTarget(card.lemma)))
            },
        )
        Choice(
            label = stringResource(id = R.string.photo_add_group),
            value = ownGroupName(card.ownGroup, form),
            enabled = hasGroups,
            onClick = {
                onAction(CandidateAction.OpenSheet(CandidatesSheetUi.CardGroup(card.lemma)))
            },
        )
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
    card: LemmaCardUiItem,
    form: DictionaryFormUi,
): String {
    val target = card.target as? CardTargetUi.Existing
    val existing = form.existingLemmas.firstOrNull { it.wordId == target?.wordId }
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
 * лексемы, под ними активные значения по компонентам в порядке значений
 * лексемы. Текст компонента — «Имя: текст», нажатие правит; у «Выбора» —
 * «Имя: подпись ▾», нажатие открывает лист опций; у компонента с несколькими
 * значениями у каждого свой крестик.
 */
@Composable
internal fun LexemeRow(
    card: LemmaCardUiItem,
    lexeme: CardLexemeUiItem,
    form: DictionaryFormUi,
    onAction: (CandidateAction) -> Unit,
    @StringRes existsMarkRes: Int = R.string.photo_add_exists_mark,
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
            Cross(onClick = { onAction(CandidateAction.DeleteLexeme(card.lemma, lexeme.key)) })
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
