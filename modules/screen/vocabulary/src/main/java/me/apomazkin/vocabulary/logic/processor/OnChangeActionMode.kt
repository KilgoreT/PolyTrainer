package me.apomazkin.vocabulary.logic.processor

import me.apomazkin.mate.Effect
import me.apomazkin.vocabulary.logic.VocabularyTabState
import me.apomazkin.vocabulary.tools.modifyFiltered

internal fun onChangeActionMode(
    state: VocabularyTabState,
    actionMode: Boolean,
    targetTermId: Long?
): Pair<VocabularyTabState, Set<Effect>> {

    val set = state.topBarState.actionState.selectedTermIds
        .toMutableSet()
    val apply = if (set.contains(targetTermId)) {
        set.remove(targetTermId)
        false
    } else {
        targetTermId?.let { set.add(it) }
        targetTermId != null
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
                        predicate = { it.id == targetTermId },
                        action = { it.copy(isSelected = apply) }
                    )
            else state.termList
                .modifyFiltered(
                    predicate = { set.contains(it.id) },
                    action = { it.copy(isSelected = false) }
                )
        ) to emptySet()
}