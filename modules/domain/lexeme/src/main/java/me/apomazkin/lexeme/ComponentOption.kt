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
 * IS486: стартовый состав опций builtin «Часть речи» (решение 2026-07-17/19:
 * состав builtin-набора — знание домена; сеются ключи, лейблы локализуются
 * ресурсами на UI).
 */
enum class PartOfSpeechOption(val key: String) {
    NOUN("noun"),
    VERB("verb"),
    ADJECTIVE("adjective"),
    ADVERB("adverb"),
    PREPOSITION("preposition"),
    PHRASE("phrase"),
}
