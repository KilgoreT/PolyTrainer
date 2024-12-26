package me.apomazkin.quiztab.logic.processor

import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarypicker.state.LangPickerState
import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult
import me.apomazkin.quiztab.logic.DatasourceEffect
import me.apomazkin.quiztab.logic.QuizTabState
import me.apomazkin.quiztab.logic.TopBarActionMsg

internal fun processTopBarActionMessage(
    state: QuizTabState,
    message: TopBarActionMsg
): ReducerResult<QuizTabState, Effect> {
    return when (message) {
        is TopBarActionMsg.AvailableDict -> onLoadAvailableDict(
            state,
            message.list
        )
        
        is TopBarActionMsg.CurrentDict -> onLoadCurrentDict(state, message.lang)
        is TopBarActionMsg.ChangeDict -> {
            val (midState, onExpandDictMenuEffects) = onExpandDictMenu(
                state,
                false
            )
            val (updatedState, onChangeDictEffects) = onChangeDict(
                midState,
                message.lang
            )
            updatedState to (onExpandDictMenuEffects + onChangeDictEffects)
        }
        
        is TopBarActionMsg.ExpandDictMenu -> onExpandDictMenu(
            state,
            message.expand
        )
    }
}

private fun onLoadAvailableDict(
    state: QuizTabState,
    dictList: List<DictUiEntity>,
): ReducerResult<QuizTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            langPickerState = state.topBarState.langPickerState?.copy(
                isLoading = false,
                availableDictList = dictList
            )
        )
    ) to setOf()

private fun onChangeDict(
    state: QuizTabState,
    dict: DictUiEntity,
): ReducerResult<QuizTabState, Effect> =
    state to setOf(DatasourceEffect.ChangeDict(lang = dict))

private fun onLoadCurrentDict(
    state: QuizTabState,
    dict: DictUiEntity,
): ReducerResult<QuizTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            langPickerState = LangPickerState(
                isLoading = false,
                currentDict = dict,
            )
        )
    ) to setOf(DatasourceEffect.LoadDictList)


private fun onExpandDictMenu(
    state: QuizTabState,
    isExpand: Boolean,
): ReducerResult<QuizTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            langPickerState = state.topBarState.langPickerState
                ?.copy(isDropDownMenuOpen = isExpand)
        )
    ) to emptySet()