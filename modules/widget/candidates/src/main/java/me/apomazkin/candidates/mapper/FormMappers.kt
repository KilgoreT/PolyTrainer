package me.apomazkin.candidates.mapper

import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.FormComponentUi
import me.apomazkin.candidates.FormGroupUi
import me.apomazkin.candidates.FormOptionUi
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.DictionaryForm
import me.apomazkin.lexeme.ExistingLemma
import me.apomazkin.lexeme.FormComponent
import me.apomazkin.lexeme.FormGroup
import me.apomazkin.lexeme.FormOption

/* Домен → UI: форма словаря и отказ записи; зовут хендлеры обоих окон. */

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

fun ExistingLemma.toUi(): ExistingLemmaUi = ExistingLemmaUi(
    wordId = wordId,
    text = text,
    lexemeCount = lexemeCount,
    coreValues = coreValues,
    lexemeTextKeys = lexemeTextKeys,
    groupNames = groupNames,
)

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
