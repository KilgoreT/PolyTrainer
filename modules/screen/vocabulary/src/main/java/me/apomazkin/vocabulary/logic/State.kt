package me.apomazkin.vocabulary.logic

import me.apomazkin.vocabulary.VocabularyTab

/**
 * IS493: state host'а вкладок словаря. Явные флаги (explicit state flags).
 *
 * Э2 (D9.1): host владеет текущим словарём и раздаёт вкладкам.
 * [dictionaryId] == null перегружен двумя смыслами — поэтому отдельный
 * [isDictResolved]: false — prefs-flow ещё не эмитил (loading-фаза),
 * true + null — честное «словарей нет».
 */
data class VocabularyHostState(
    val selectedTab: VocabularyTab = VocabularyTab.WORDS,
    val dictionaryId: Long? = null,
    val isDictResolved: Boolean = false,
)
