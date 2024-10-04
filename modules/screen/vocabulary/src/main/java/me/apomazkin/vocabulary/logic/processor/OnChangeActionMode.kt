package me.apomazkin.vocabulary.logic.processor

import me.apomazkin.mate.Effect
import me.apomazkin.vocabulary.entity.WordInfo
import me.apomazkin.vocabulary.logic.VocabularyTabState
import me.apomazkin.vocabulary.tools.modifyFiltered

internal fun onChangeActionMode(
    state: VocabularyTabState,
    actionMode: Boolean,
    targetWord: WordInfo?
): Pair<VocabularyTabState, Set<Effect>> {

    val set = state.topBarState.actionState.selectedTermIds
        .toMutableSet()
    val apply = if (set.contains(targetWord)) {
        set.remove(targetWord)
        false
    } else {
        targetWord?.let { set.add(it) }
        targetWord != null
    }

    return state
        .copy(
            topBarState = state.topBarState.copy(
                isActionMode = actionMode && set.isNotEmpty(),
                actionState = state.topBarState.actionState.copy(
                    selectedTermIds = if (actionMode) set else emptySet()
                )
            ),
            termList = if (actionMode && set.isNotEmpty())
                state.termList
                    .modifyFiltered(
                        predicate = { it.id == targetWord?.id },
                        action = { it.copy(isSelected = apply) }
                    )
            else state.termList
                .modifyFiltered(
                    predicate = { term -> set.any { word -> word.id == term.id } },
                    action = { it.copy(isSelected = false) }
                )
        ) to emptySet()
}