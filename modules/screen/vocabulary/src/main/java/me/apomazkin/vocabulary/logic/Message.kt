package me.apomazkin.vocabulary.logic

import me.apomazkin.vocabulary.VocabularyTab

sealed interface Msg {
    data class SelectTab(
        val tab: VocabularyTab,
    ) : Msg

    /**
     * IS493 Э2 (D9.1): эмиссия prefs-flow текущего словаря
     * ([CurrentDictFlowHandler]); null — словарей нет (валидно).
     */
    data class DictionaryChanged(
        val dictionaryId: Long?,
    ) : Msg
}
