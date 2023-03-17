package me.apomazkin.vocabulary.logic

import android.util.Log
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.logic.processor.processTopBarActionMessage
import me.apomazkin.vocabulary.logic.processor.processUiMessage
import me.apomazkin.vocabulary.logic.processor.processWordDetailMessage
import me.apomazkin.vocabulary.tools.modifyFiltered

internal class VocabularyTabReducer : MateReducer<VocabularyTabState, Msg, Effect> {
    override fun reduce(
        state: VocabularyTabState,
        message: Msg
    ): ReducerResult<VocabularyTabState, Effect> {
        Log.d("##MATE##", "Reduce --prevState--: $state ")
        Log.d("##MATE##", "Reduce ---message---: $message ")
        return when (message) {
            is TopBarActionMsg -> processTopBarActionMessage(state, message)
            is Msg.TermDataLoad -> onTermDataLoad(state)
            is Msg.TermDataLoaded -> onTermDataLoaded(state, message.termList)
            is Msg.ExpandTerm -> onExpandTerm(state, message.targetId, message.expand)
            is Msg.AddWordWidget -> onOpenAddWordWidget(state, message.show)
            is Msg.WordValueChange -> onWordValueChange(state, message.value)
            is Msg.EnableAddWordDetail -> onEnableAddWordDetail(state, message.value)
            is Msg.AddWord -> onAddWord(state, message.value)
            is Msg.DropDownMenu -> onOpenDropDownMenu(state, message.wordId, message.isOpen)
            is Msg.DeleteWord -> onDeleteWord(state, message.wordId, message.wordValue)
            is Msg.DeleteLexeme -> onDeleteLexeme(state, message.lexemeId)
            is WordDetailMsg -> processWordDetailMessage(state, message)
            is UiMsg -> processUiMessage(state, message)
            Msg.Empty -> state to emptySet()
        }.also {
            Log.d("##MATE##", "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                Log.d("##MATE##", "Reduce --toEffect--: $effect ")
            }
        }
    }

    private fun onTermDataLoad(
        state: VocabularyTabState,
    ): ReducerResult<VocabularyTabState, Effect> = state
        .copy(isLoading = true) to setOf(DatasourceEffect.LoadTermData)

    private fun onTermDataLoaded(
        state: VocabularyTabState,
        termList: List<TermUiItem>,
    ): ReducerResult<VocabularyTabState, Effect> = state
        .copy(
            isLoading = false,
            termList = termList
        ) to emptySet()

    private fun onOpenDropDownMenu(
        state: VocabularyTabState,
        wordId: Long,
        open: Boolean
    ): ReducerResult<VocabularyTabState, DatasourceEffect> = state
        .copy(termList = state.termList
            .modifyFiltered(
                predicate = { it.id == wordId },
                action = { it.copy(isDropDownOpen = open) }
            )
        ) to emptySet()

    private fun onExpandTerm(
        state: VocabularyTabState,
        itemId: Long,
        expand: Boolean
    ): ReducerResult<VocabularyTabState, Effect> = state
        .copy(
            termList = state.termList
                .modifyFiltered(
                    predicate = { it.id == itemId },
                    action = { it.copy(isExpand = expand) },
                )
        ) to emptySet()

    private fun onOpenAddWordWidget(
        state: VocabularyTabState,
        show: Boolean,
    ): ReducerResult<VocabularyTabState, Effect> = state
        .copy(
            addWordDialogState = state.addWordDialogState.copy(
                isAddWordWidgetOpen = show,
                addWordValue = EMPTY_STRING,
            )
        ) to emptySet()

    private fun onWordValueChange(
        state: VocabularyTabState,
        newValue: String
    ): ReducerResult<VocabularyTabState, Effect> = state
        .copy(
            addWordDialogState = state.addWordDialogState.copy(addWordValue = newValue)
        ) to emptySet()

    private fun onEnableAddWordDetail(
        state: VocabularyTabState,
        isEnable: Boolean
    ): ReducerResult<VocabularyTabState, Effect> = state
        .copy(
            addWordDialogState = state.addWordDialogState.copy(isAddDetailEnable = isEnable)
        ) to emptySet()

    private fun onAddWord(
        state: VocabularyTabState,
        value: String
    ): ReducerResult<VocabularyTabState, Effect> =
        state to setOf(
            DatasourceEffect.AddWord(value),
            UiEffect.ShowSnackbar(title = "Word \"${value}\" is added")
        )

    private fun onDeleteWord(
        state: VocabularyTabState,
        wordId: Long,
        wordValue: String
    ): ReducerResult<VocabularyTabState, Effect> =
        state to setOf(
            DatasourceEffect.DeleteWord(id = wordId),
            UiEffect.ShowSnackbar(title = "Word \"${wordValue}\" is deleted")
        )

    private fun onDeleteLexeme(
        state: VocabularyTabState,
        lexemeId: Long
    ): ReducerResult<VocabularyTabState, Effect> = state.copy(
        wordDetailDialogState = state.wordDetailDialogState.copy(
            lexemeList = state.wordDetailDialogState.lexemeList.filterNot { it.lexemeId == lexemeId }
        )
    ) to setOf(
        DatasourceEffect.DeleteLexeme(lexemeId)
    )
}