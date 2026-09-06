package me.apomazkin.vocabulary.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.ReducerResult

/**
 * IS493: reducer host'а. Эффектов нет — сброс ActionMode words при
 * переключении вкладки делает app-мост (stage1_design_tree D4.2), host о
 * words-домене не знает; подписка на словарь — long-running flow
 * ([CurrentDictFlowHandler]), не эффект.
 */
class VocabularyHostReducer : MateReducer<VocabularyHostState, Msg, Effect> {

    override fun reduce(
        state: VocabularyHostState,
        message: Msg,
    ): ReducerResult<VocabularyHostState, Effect> = when (message) {
        is Msg.SelectTab -> state.copy(selectedTab = message.tab) to emptySet()
        // Первая эмиссия (включая null) переводит isDictResolved в true —
        // с этого момента null означает «словарей нет», не «ещё грузимся».
        // Первая эмиссия (включая null) переводит isDictResolved в true —
        // с этого момента null означает «словарей нет», не «ещё грузимся».
        is Msg.DictionaryChanged -> state.copy(
            dictionaryId = message.dictionaryId,
            isDictResolved = true,
        ) to emptySet()
    }
}
