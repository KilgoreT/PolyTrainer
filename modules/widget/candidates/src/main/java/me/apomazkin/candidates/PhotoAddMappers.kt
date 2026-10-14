package me.apomazkin.candidates

import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.BatchWord
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch

/*
 * IS532: маппер границы Ш4 — содержимое окна → пакет записи (AWP-20,
 * AWP-22). Правила одни и для пометки «не попадёт в ядро» (AWP-11).
 * IS533: общий для окна снимка и диалога в карточке слова.
 */

/** Подпись примеров источника в компоненте примеров (AWP-22.2). */
const val EXAMPLE_CAPTION = "Wiktionary"

/**
 * Пакет из окна (AWP-20): каждая карточка с лексемами без «есть» — слово
 * пакета (новое или существующее по выбору «куда класть»), её группа —
 * своя или общая; лексемы «есть» пропускаются (AWP-20.4).
 * @param commonGroupId общая группа окна или null («Все»).
 */
fun wordsBatchOf(
    cards: List<PhotoCard>,
    mapping: PhotoMapping,
    form: PhotoDictionaryForm,
    commonGroupId: Long?,
): WordsBatch {
    val words = cards.mapNotNull { card ->
        val lexemes = card.lexemes.filterNot { it.exists }
        if (lexemes.isEmpty()) return@mapNotNull null
        BatchWord(
                wordId = (card.target as? PhotoCardTarget.Existing)?.wordId,
                text = card.lemma,
                groupId = card.groupIdOr(commonGroupId),
                lexemes = lexemes.map { BatchLexeme(components = it.componentsFor(mapping, form)) },
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
    cards: List<PhotoCard>,
    mapping: PhotoMapping,
    form: PhotoDictionaryForm,
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
                        lexemes = lexemes.map { BatchLexeme(components = it.componentsFor(mapping, form)) },
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
    cards: List<PhotoCard>,
    mapping: PhotoMapping,
    form: PhotoDictionaryForm,
): WordsBatch {
    val targetKeys = form.existingLemmas.firstOrNull { it.wordId == targetWordId }?.lexemeTextKeys.orEmpty()
    val lexemes = cards.flatMap { card -> card.lexemes.filterNot { it.exists || it.matchesTexts(targetKeys) } }
    return WordsBatch(
            dictionaryId = form.dictionaryId,
            words = listOf(
                    BatchWord(
                            wordId = targetWordId,
                            text = targetText,
                            groupId = null,
                            lexemes = lexemes.map { BatchLexeme(components = it.componentsFor(mapping, form)) },
                            absorbWordId = absorbWordId,
                    ),
            ),
    )
}

/** Группа леммы (AWP-08): своя у карточки, иначе общая; «Все» — без группы. */
fun PhotoCard.groupIdOr(commonGroupId: Long?): Long? = when (val own = ownGroup) {
    PhotoOwnGroup.AsCommon -> commonGroupId
    PhotoOwnGroup.All -> null
    is PhotoOwnGroup.Group -> own.groupId
}

/**
 * Значения компонентов лексемы по соответствиям (AWP-22): текст перевода —
 * в компонент перевода, толкование — в свой; толкование без перевода идёт
 * в одно место — в свой компонент, если он ядро, иначе в компонент
 * перевода (AWP-22.3…22.5); часть речи — опцией; оставшиеся примеры — в
 * компонент примеров с подписью источника; пустые тексты не создаются
 * (AWP-20.6).
 */
fun PhotoCardLexeme.componentsFor(mapping: PhotoMapping, form: PhotoDictionaryForm): List<BatchComponent> {
    val components = mutableListOf<BatchComponent>()
    val text = translation.text.trim()
    if (hasOwnTranslations) {
        components += textValue(mapping.translation, text)
        components += textValue(mapping.gloss, gloss?.text?.trim().orEmpty())
    } else {
        components += textValue(glossOnlyTarget(mapping, form), text)
    }
    val optionId = partOfSpeechOptionId
    val partOfSpeech = form.componentOf(mapping.partOfSpeech)
    if (optionId != null && partOfSpeech != null) {
        components += BatchComponent(typeId = partOfSpeech.id, data = ChoiceValues(optionId))
    }
    val exampleTarget = form.componentOf(mapping.examples)
    if (exampleTarget != null) {
        examples.map { it.text.trim() }.filter { it.isNotEmpty() }.forEach { example ->
            components += BatchComponent(
                    typeId = exampleTarget.id,
                    data = CaptionedTextValues(text = Primitive.Text(example), caption = Primitive.Text(EXAMPLE_CAPTION)),
            )
        }
    }
    return components
}

/** Цель толкования без перевода: компонент толкования, если он ядро, иначе компонент перевода. */
private fun glossOnlyTarget(mapping: PhotoMapping, form: PhotoDictionaryForm): PhotoTarget {
    val glossComponent = form.componentOf(mapping.gloss)
    return if (glossComponent != null && glossComponent.isCore) mapping.gloss else mapping.translation
}

private fun textValue(target: PhotoTarget, text: String): List<BatchComponent> {
    if (text.isEmpty() || target !is PhotoTarget.Component) return emptyList()
    return listOf(BatchComponent(typeId = target.componentId, data = TextValues(Primitive.Text(text))))
}
