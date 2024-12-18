package me.apomazkin.dictionarytab.logic.processor

import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.VocabularyTabState
import me.apomazkin.dictionarytab.tools.modifyFiltered
import me.apomazkin.mate.Effect

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
            termList = state.termList
                .modifyFiltered(
                    predicate = { it.id == targetWord?.id },
                    action = { it.copy(isSelected = apply) }
                )
                .modifyFiltered(
                    predicate = { !actionMode },
                    action = { it.copy(isSelected = false) }
                )
        ) to emptySet()
}