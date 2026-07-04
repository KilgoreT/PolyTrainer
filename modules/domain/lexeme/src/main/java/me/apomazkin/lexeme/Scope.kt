package me.apomazkin.lexeme

/**
 * Область видимости user-defined компонента.
 *
 * - [Global] — `dictionaryId IS NULL` (виден во всех словарях);
 * - [PerDictionaries] — одна или несколько строк, по одной на каждый `dictionaryId`.
 *
 * Инвариант `global ⊥ per-dict` (F039) — у одного и того же `name` не может
 * одновременно существовать `Global` строка и `PerDictionaries` строка.
 */
sealed interface Scope {
    data object Global : Scope
    data class PerDictionaries(val ids: List<Long>) : Scope
}
