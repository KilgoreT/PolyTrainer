package me.apomazkin.lexeme

import java.util.Date

/**
 * IS486: опция CHOICE-компонента.
 *
 * [id] — адрес опции; на него ссылаются зависимости ([DependencyTarget.Option])
 *   и значения ([ChoiceValues]).
 * [componentTypeId] — чья опция.
 * [systemKey] — стабильный ключ builtin-опции (см. [PartOfSpeechOption.key]);
 *   null → пользовательская опция. Display builtin-опции резолвится из ресурсов
 *   по ключу — дом-паттерн builtin-компонентов (name-override).
 * [label] — текст опции; для builtin-опций null (или override поверх ключа),
 *   для пользовательских обязателен. Display = label ?: ресурс(systemKey).
 * [position] — порядок в списке опций.
 * [removedAt] — soft-delete в стиле остальных сущностей; null → живая.
 */
data class ComponentOption(
    val id: Long,
    val componentTypeId: ComponentTypeId,
    val systemKey: String? = null,
    val label: String? = null,
    val position: Int,
    val removedAt: Date? = null,
)

/**
 * Состав опций builtin «Часть речи»: состав builtin-набора — знание
 * домена; сеются ключи, лейблы локализуются ресурсами на UI.
 *
 * Кроме частей речи в том же слоте — типы многословных единиц (фразовый
 * глагол, коллокация, идиома), как в учебных словарях. Порядок элементов —
 * порядок в списке выбора: `ordinal` = `position` при засеве нового
 * словаря (существующие словари переставляет миграция 14→15). Ключи
 * неизменны — на них ссылаются миграции.
 */
enum class PartOfSpeechOption(val key: String) {
    NOUN("noun"),
    VERB("verb"),
    ADJECTIVE("adjective"),
    ADVERB("adverb"),
    PRONOUN("pronoun"),
    NUMERAL("numeral"),
    PREPOSITION("preposition"),
    INTERJECTION("interjection"),
    PHRASAL_VERB("phrasal_verb"),
    COLLOCATION("collocation"),
    IDIOM("idiom"),
    PHRASE("phrase"),
}
