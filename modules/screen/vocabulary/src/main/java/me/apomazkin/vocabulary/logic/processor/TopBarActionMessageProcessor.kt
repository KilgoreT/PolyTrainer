package me.apomazkin.vocabulary.logic.processor

import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult
import me.apomazkin.vocabulary.entity.LangUiEntity
import me.apomazkin.vocabulary.logic.DatasourceEffect
import me.apomazkin.vocabulary.logic.TopBarActionMsg
import me.apomazkin.vocabulary.logic.VocabularyTabState

internal fun processTopBarActionMessage(
    state: VocabularyTabState,
    message: TopBarActionMsg
): ReducerResult<VocabularyTabState, Effect> {
    return when (message) {
        is TopBarActionMsg.AvailableLang -> onLoadAvailableLang(state, message.list)
        is TopBarActionMsg.CurrentLang -> onLoadCurrentLang(state, message.numericCode)
        is TopBarActionMsg.ChangeLang -> onChangeLang(state, message.numericCode)
        is TopBarActionMsg.ExpandLangMenu -> onExpandLangMenu(state, message.expand)
    }
}

private fun onLoadAvailableLang(
    state: VocabularyTabState,
    langList: List<LangUiEntity>,
): ReducerResult<VocabularyTabState, Effect> = state
    .copy(
        topBarActionState = state.topBarActionState.copy(
            isLoading = state.topBarActionState.currentLang == null,
            availableLangList = langList
        )
    ) to setOf(DatasourceEffect.LoadCurrentLang)

private fun onChangeLang(
    state: VocabularyTabState,
    numericCode: Int,
): ReducerResult<VocabularyTabState, Effect> =
    state to setOf(DatasourceEffect.ChangeLang(numericCode = numericCode))

private fun onLoadCurrentLang(
    state: VocabularyTabState,
    numericCode: Int,
): ReducerResult<VocabularyTabState, Effect> = state
    .copy(
        topBarActionState = state.topBarActionState.copy(
            isLoading = false,
            currentLang = state.topBarActionState.availableLangList
                .first { it.numericCode == numericCode }
        )
    ) to setOf(DatasourceEffect.LoadTermData)


private fun onExpandLangMenu(
    state: VocabularyTabState,
    isExpand: Boolean,
): ReducerResult<VocabularyTabState, Effect> = state
    .copy(
        topBarActionState = state.topBarActionState.copy(isDropDownMenuOpen = isExpand)
    ) to emptySet()