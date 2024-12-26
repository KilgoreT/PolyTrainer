package me.apomazkin.dictionarytab.logic.processor

import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarypicker.state.LangPickerState
import me.apomazkin.dictionarytab.logic.DatasourceEffect
import me.apomazkin.dictionarytab.logic.DictionaryTabState
import me.apomazkin.dictionarytab.logic.TopBarActionMsg
import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult

internal fun processTopBarActionMessage(
    state: DictionaryTabState,
    message: TopBarActionMsg
): ReducerResult<DictionaryTabState, Effect> {
    return when (message) {
        is TopBarActionMsg.AvailableDict -> onLoadAvailableDict(state, message.list)
        is TopBarActionMsg.CurrentDict -> onLoadCurrentDict(state, message.lang)
        is TopBarActionMsg.ChangeDict -> {
            val (midState, onExpandDictMenuEffects) = onExpandDictMenu(state, false)
            val (updatedState, onChangeDictEffects) = onChangeDict(
                midState,
                message.lang
            )
            updatedState to (onExpandDictMenuEffects + onChangeDictEffects)
        }

        is TopBarActionMsg.ExpandDictMenu -> onExpandDictMenu(state, message.expand)
    }
}

private fun onLoadAvailableDict(
    state: DictionaryTabState,
    dictList: List<DictUiEntity>,
): ReducerResult<DictionaryTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            langPickerState = state.topBarState.langPickerState?.copy(
                isLoading = false,
                availableDictList = dictList
            )
        )
    ) to setOf(DatasourceEffect.LoadTermData)

private fun onChangeDict(
    state: DictionaryTabState,
    dict: DictUiEntity,
): ReducerResult<DictionaryTabState, Effect> =
    state to setOf(DatasourceEffect.ChangeDict(lang = dict))

private fun onLoadCurrentDict(
    state: DictionaryTabState,
    dict: DictUiEntity,
): ReducerResult<DictionaryTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            langPickerState = LangPickerState(
                isLoading = false,
                currentDict = dict,
            )
        )
    ) to setOf(DatasourceEffect.LoadDictList)


private fun onExpandDictMenu(
    state: DictionaryTabState,
    isExpand: Boolean,
): ReducerResult<DictionaryTabState, Effect> = state
    .copy(
        topBarState = state.topBarState.copy(
            langPickerState = state.topBarState.langPickerState
                ?.copy(isDropDownMenuOpen = isExpand)
        )
    ) to emptySet()