package me.apomazkin.wordstab.logic.photo

import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.BatchWord
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.logic.PhotoWordsState

/*
 * IS532: маппер границы Ш4 — содержимое окна → пакет записи (AWP-20,
 * AWP-22). Правила одни и для пометки «не попадёт в ядро» (AWP-11).
 */

/** Подпись примеров источника в компоненте примеров (AWP-22.2). */
internal const val EXAMPLE_CAPTION = "Wiktionary"

/**
 * Пакет из окна (AWP-20): каждая карточка с лексемами без «есть» — слово
 * пакета (новое или существующее по выбору «куда класть»), её группа —
 * своя или общая; лексемы «есть» пропускаются (AWP-20.4). Нет формы —
 * пустой пакет.
 */
internal fun PhotoWordsState.toWordsBatch(): WordsBatch {
    val form = form ?: return WordsBatch(dictionaryId = dictionaryId ?: 0L, words = emptyList())
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

/** Группа леммы (AWP-08): своя у карточки, иначе общая; «Все» — без группы. */
internal fun PhotoCard.groupIdOr(commonGroupId: Long?): Long? = when (val own = ownGroup) {
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
internal fun PhotoCardLexeme.componentsFor(mapping: PhotoMapping, form: PhotoDictionaryForm): List<BatchComponent> {
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
