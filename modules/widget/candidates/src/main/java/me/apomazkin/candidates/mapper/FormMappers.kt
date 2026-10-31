package me.apomazkin.candidates.mapper

import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidateValueUiItem
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.EditableTextUiItem
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.FormComponentUi
import me.apomazkin.candidates.FormGroupUi
import me.apomazkin.candidates.FormOptionUi
import me.apomazkin.candidates.SavedLexemeUi
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.DictionaryForm
import me.apomazkin.lexeme.ExistingLemma
import me.apomazkin.lexeme.FormComponent
import me.apomazkin.lexeme.FormGroup
import me.apomazkin.lexeme.FormOption
import me.apomazkin.lexeme.ImageValues
import me.apomazkin.lexeme.SavedLexeme
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues

/* Домен → UI: форма словаря, леммы словаря с сохранёнными лексемами и отказ записи; зовут хендлеры обоих окон. */

fun DictionaryForm.toUi(): DictionaryFormUi = DictionaryFormUi(
    dictionaryId = dictionaryId,
    translationLanguage = translationLanguage,
    components = components.map { it.toUi() },
    options = options.mapValues { (_, options) -> options.map { it.toUi() } },
    groups = groups.map { it.toUi() },
    existingLemmas = existingLemmas.map { it.toUi() },
)

fun FormComponent.toUi(): FormComponentUi = FormComponentUi(
    id = id,
    systemKey = systemKey,
    name = name,
    template = template,
    isCore = isCore,
    isMultiple = isMultiple,
    position = position,
    dependsOn = dependsOn,
)

fun FormOption.toUi(): FormOptionUi = FormOptionUi(
    id = id,
    key = key,
    label = label,
    position = position,
)

fun FormGroup.toUi(): FormGroupUi = FormGroupUi(
    id = id,
    name = name,
)

/** Лемма словаря с датой добавления и сохранёнными лексемами (AWP-18.10, AWP-46). */
fun ExistingLemma.toUi(): ExistingLemmaUi = ExistingLemmaUi(
    wordId = wordId,
    text = text,
    lexemeCount = lexemeCount,
    coreValues = coreValues,
    lexemeTextKeys = lexemeTextKeys,
    groupNames = groupNames,
    addedAt = addedAt,
    lexemes = lexemes.map { it.toUi() },
)

/**
 * Значения сохранённой лексемы по компонентам в порядке первого появления
 * (AWP-46): опции → «Выбор», тексты и «текст с подписью» → текст; все
 * значения одного компонента — в одно. Компоненты — только формы: их отобрал
 * загрузчик; порядок карточки задаёт экран по форме (AWP-44).
 */
fun SavedLexeme.toUi(): SavedLexemeUi = SavedLexemeUi(
    id = id,
    values = components.groupBy { it.typeId }.mapNotNull { (typeId, own) -> savedValueOf(typeId, own) },
)

private fun savedValueOf(
    typeId: Long,
    own: List<BatchComponent>,
): CandidateValueUiItem? {
    val options = own.mapNotNull { (it.data as? ChoiceValues)?.optionId }
    if (options.isNotEmpty()) return CandidateValueUiItem.Choice(componentId = typeId, options = options)
    val texts = own.mapNotNull { textOf(it.data) }
    if (texts.isEmpty()) return null
    return CandidateValueUiItem.Text(
        componentId = typeId,
        texts = texts.map { EditableTextUiItem.of(it) },
        field = null,
    )
}

private fun textOf(data: TemplateValues): String? = when (data) {
    is TextValues -> data.value.value
    is CaptionedTextValues -> data.text.value
    is ChoiceValues, is ImageValues -> null
}

/** Отказ записи до первой вставки → причина для экрана (AWP-21, WCL-13). */
fun AddWordsBatchOutcome.Rejected.toUi(): AddFailureUi = when (this) {
    is AddWordsBatchOutcome.Rejected.GroupMissing -> AddFailureUi.GroupMissing(groupId)
    is AddWordsBatchOutcome.Rejected.WordMissing -> AddFailureUi.WordMissing(wordId)
    is AddWordsBatchOutcome.Rejected.TypeUnavailable -> AddFailureUi.TypeUnavailable(typeId)
    is AddWordsBatchOutcome.Rejected.NoCoreValue -> AddFailureUi.NoCoreValue
    AddWordsBatchOutcome.Rejected.DictionaryMissing -> AddFailureUi.DictionaryMissing
}

/** Причина с id для сообщений на случай: группа, слово или компонент исчезли (AWP-21). */
fun AddWordsBatchOutcome.Rejected.GroupMissing.toUi(): AddFailureUi.GroupMissing =
    AddFailureUi.GroupMissing(groupId)

fun AddWordsBatchOutcome.Rejected.WordMissing.toUi(): AddFailureUi.WordMissing =
    AddFailureUi.WordMissing(wordId)

fun AddWordsBatchOutcome.Rejected.TypeUnavailable.toUi(): AddFailureUi.TypeUnavailable =
    AddFailureUi.TypeUnavailable(typeId)
