package me.apomazkin.vocabulary.logic.processor

import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult
import me.apomazkin.vocabulary.entity.DictUiEntity
import me.apomazkin.vocabulary.logic.DatasourceEffect
import me.apomazkin.vocabulary.logic.TopBarActionMsg
import me.apomazkin.vocabulary.logic.VocabularyTabState

internal fun processTopBarActionMessage(
    state: VocabularyTabState,
    message: TopBarActionMsg
): ReducerResult<VocabularyTabState, Effect> {
    return when (message) {
        is TopBarActionMsg.AvailableDict -> onLoadAvailableDict(state, message.list)
        is TopBarActionMsg.CurrentDict -> onLoadCurrentDict(state, message.numericCode)
        is TopBarActionMsg.ChangeDict -> onChangeDict(state, message.numericCode)
        is TopBarActionMsg.ExpandDictMenu -> onExpandDictMenu(state, message.expand)
    }
}

private fun onLoadAvailableDict(
    state: VocabularyTabState,
    dictList: List<DictUiEntity>,
): ReducerResult<VocabularyTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            mainState = state.topBarState.mainState.copy(
                isLoading = state.topBarState.mainState.currentDict == null,
                availableDictList = dictList
            )
        )
    ) to setOf(DatasourceEffect.LoadCurrentDict)

private fun onChangeDict(
    state: VocabularyTabState,
    numericCode: Int,
): ReducerResult<VocabularyTabState, Effect> =
    state to setOf(DatasourceEffect.ChangeDict(numericCode = numericCode))

private fun onLoadCurrentDict(
    state: VocabularyTabState,
    numericCode: Int,
): ReducerResult<VocabularyTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            mainState = state.topBarState.mainState.copy(
                isLoading = false,
                currentDict = state.topBarState.mainState.availableDictList
                    .first { it.numericCode == numericCode }
            )
        )
    ) to setOf(DatasourceEffect.LoadTermData)


private fun onExpandDictMenu(
    state: VocabularyTabState,
    isExpand: Boolean,
): ReducerResult<VocabularyTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            mainState = state.topBarState.mainState
                .copy(isDropDownMenuOpen = isExpand)
        )
    ) to emptySet()