package me.apomazkin.candidates

import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.BatchWord
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch

/*
 * IS532/IS536: содержимое окна → пакет записи (AWP-20, AWP-32) — сборка
 * чистой функцией там же, где UI-типы; общая для окна снимка и диалога
 * карточки слова.
 */

/** Подпись значения из ответа в компоненте «текст с подписью» (AWP-32). */
const val SOURCE_CAPTION = "Wiktionary"

/**
 * Пакет из окна (AWP-20): каждая карточка с лексемами без «есть» — слово
 * пакета (новое или существующее по выбору «куда класть»), её группа —
 * своя или общая; лексемы «есть» пропускаются (AWP-20.4).
 * @param commonGroupId общая группа окна или null («Все»).
 */
fun wordsBatchOf(
    cards: List<LemmaCardUiItem>,
    form: DictionaryFormUi,
    commonGroupId: Long?,
): WordsBatch {
    val words = cards.mapNotNull { card ->
        val lexemes = card.lexemes.filterNot { it.exists }
        if (lexemes.isEmpty()) return@mapNotNull null
        BatchWord(
            wordId = (card.target as? CardTargetUi.Existing)?.wordId,
            text = card.lemma,
            groupId = card.groupIdOr(commonGroupId),
            lexemes = lexemes.map { BatchLexeme(components = it.componentsOf(form)) },
        )
    }
    return WordsBatch(dictionaryId = form.dictionaryId, words = words)
}

/**
 * Пакет из диалога в карточке слова (WCL-10): все лексемы-кандидаты без
 * «есть» из всех разделов — в одно слово по id, без группы; пустой список
 * слов, если писать нечего.
 */
fun wordsBatchForWord(
    wordId: Long,
    text: String,
    cards: List<LemmaCardUiItem>,
    form: DictionaryFormUi,
    renameTo: String? = null,
): WordsBatch {
    val lexemes = cards.flatMap { card -> card.lexemes.filterNot { it.exists } }
    val words = if (lexemes.isEmpty()) {
        emptyList()
    } else {
        listOf(
            BatchWord(
                wordId = wordId,
                text = text,
                groupId = null,
                lexemes = lexemes.map { BatchLexeme(components = it.componentsOf(form)) },
                renameTo = renameTo,
            ),
        )
    }
    return WordsBatch(dictionaryId = form.dictionaryId, words = words)
}

/**
 * Пачка переноса (WCL-15.3): лексемы-кандидаты всех разделов без пометки
 * «есть» и без тех, что уже есть у слова-получателя, — в слово
 * [targetWordId] словаря; слово диалога [absorbWordId] отдаёт ему свои
 * лексемы и удаляется той же транзакцией. Слово пачки есть и без лексем —
 * перенос нужен сам по себе.
 */
fun wordsBatchForTransfer(
    targetWordId: Long,
    targetText: String,
    absorbWordId: Long,
    cards: List<LemmaCardUiItem>,
    form: DictionaryFormUi,
): WordsBatch {
    val targetKeys = form.existingLemmas.firstOrNull { it.wordId == targetWordId }?.lexemeTextKeys.orEmpty()
    val lexemes = cards.flatMap { card -> card.lexemes.filterNot { it.exists || it.matchesTexts(targetKeys, form) } }
    return WordsBatch(
        dictionaryId = form.dictionaryId,
        words = listOf(
            BatchWord(
                wordId = targetWordId,
                text = targetText,
                groupId = null,
                lexemes = lexemes.map { BatchLexeme(components = it.componentsOf(form)) },
                absorbWordId = absorbWordId,
            ),
        ),
    )
}

/** Группа леммы (AWP-08): своя у карточки, иначе общая; «Все» — без группы. */
fun LemmaCardUiItem.groupIdOr(commonGroupId: Long?): Long? = when (val own = ownGroup) {
    CardOwnGroupUi.AsCommon -> commonGroupId
    CardOwnGroupUi.All -> null
    is CardOwnGroupUi.Group -> own.groupId
}

/**
 * Значения компонентов лексемы по сопоставлению и правкам (AWP-32):
 * активные значения с содержимым — текстовому компоненту текст, в
 * компоненте «текст с подписью» с подписью «Wiktionary», «Выбору» — опцию;
 * у «несколько» — все оставшиеся, иначе первое; пустые тексты и неактивные
 * значения не пишутся.
 */
fun CardLexemeUiItem.componentsOf(form: DictionaryFormUi): List<BatchComponent> = values
    .filter { it.isActive }
    .flatMap { value ->
        val component = form.componentOf(value.componentId) ?: return@flatMap emptyList()
        when (value) {
            is CandidateValueUiItem.Text -> value.texts
                .map { it.text.trim() }
                .filter { it.isNotEmpty() }
                .let { texts -> if (component.isMultiple) texts else texts.take(1) }
                .mapNotNull { text -> dataOf(component.template, text)?.let { BatchComponent(typeId = component.id, data = it) } }
            is CandidateValueUiItem.Choice -> value.options
                .let { options -> if (component.isMultiple) options else options.take(1) }
                .map { option -> BatchComponent(typeId = component.id, data = ChoiceValues(option)) }
        }
    }

private fun dataOf(
    template: ComponentTemplate,
    text: String,
): TemplateValues? = when (template) {
    ComponentTemplate.TEXT -> TextValues(Primitive.Text(text))
    ComponentTemplate.CAPTIONED_TEXT -> CaptionedTextValues(text = Primitive.Text(text), caption = Primitive.Text(SOURCE_CAPTION))
    ComponentTemplate.CHOICE, ComponentTemplate.IMAGE -> null
}
