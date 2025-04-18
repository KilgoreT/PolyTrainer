package me.apomazkin.dictionarytab.logic

import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.processor.onChangeActionMode
import me.apomazkin.dictionarytab.logic.processor.processTopBarActionMessage
import me.apomazkin.dictionarytab.logic.processor.processUiMessage
import me.apomazkin.dictionarytab.tools.modifyFiltered
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.ui.logger.LexemeLogger

internal class VocabularyTabReducer(
    val logger: LexemeLogger,
) : MateReducer<DictionaryTabState, Msg, Effect> {
    override fun reduce(
        state: DictionaryTabState,
        message: Msg
    ): ReducerResult<DictionaryTabState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            is TopBarActionMsg -> processTopBarActionMessage(state = state, message = message)
            is Msg.TermDataLoad -> onTermDataLoad(state = state)
            is Msg.TermDataLoaded -> onTermDataLoaded(state = state, termList = message.termList)
            is Msg.ChangeActionMode -> onChangeActionMode(
                state = state,
                actionMode = message.isActionMode,
                targetWord = message.targetWord
            )

            is Msg.StartAddWord -> onOpenAddWordWidget(
                state = state,
                show = message.show,
                wordValue = message.wordValue,
            )

            is Msg.StartChangeWord -> {
                val (midState, onChangeActionModeEffects) = onChangeActionMode(
                    state = state,
                    actionMode = false,
                    targetWord = null,
                )
                val (updatedState, onChangeWordEffects) = onOpenAddWordWidget(
                    state = midState,
                    show = true,
                    wordId = message.wordId,
                    wordValue = message.wordValue,
                )
                updatedState to (onChangeActionModeEffects + onChangeWordEffects)
            }

            is Msg.WordValueChange -> onWordValueChange(state = state, newValue = message.value)

            is Msg.AddWord -> {
                val (midState, accEffects) = onOpenAddWordWidget(
                    state = state,
                    show = false,
                )
                val (updatedState, onAddWordEffects) = onAddWord(midState, message.value)
                updatedState to (accEffects + onAddWordEffects)
            }

            is Msg.ChangeWord -> {
                val (changeActionModeState, onChangeActionModeEffects) = onChangeActionMode(
                    state = state,
                    actionMode = false,
                    targetWord = null,
                )
                val (openAddWordState, openAddWordEffects) = onOpenAddWordWidget(
                    state = changeActionModeState,
                    show = false,
                )
                val (onChangeWordState, onChangeWordEffects) = onChangeWord(
                    state = openAddWordState,
                    wordId = message.wordId,
                    value = message.value
                )
                onChangeWordState to
                        (onChangeActionModeEffects + openAddWordEffects + onChangeWordEffects)
            }

            is Msg.ConfirmDeleteWordDialog -> onConfirmDeleteWordDialog(
                state = state,
                isOpen = message.isOpen,
                wordIds = message.wordIds,
            )

            is Msg.DeleteWord -> {
                val (confirmDialogState, confirmDialogEffects) = onConfirmDeleteWordDialog(
                    state = state,
                    isOpen = false,
                    wordIds = message.wordIds,
                )
                val (changeActionModeState, changeActionModeEffect) = onChangeActionMode(
                    state = confirmDialogState,
                    actionMode = false,
                    targetWord = null,
                )
                val (deleteWordState, deleteWordEffects) = onDeleteWord(
                    state = changeActionModeState,
                    wordIds = message.wordIds,
                )
                deleteWordState to
                        (confirmDialogEffects + changeActionModeEffect + deleteWordEffects)
            }
            is UiMsg -> processUiMessage(state, message)
            Msg.Empty -> state to emptySet()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }

    private fun onTermDataLoad(
        state: DictionaryTabState,
    ): ReducerResult<DictionaryTabState, Effect> = state
        .copy(isLoading = true) to setOf(DatasourceEffect.LoadTermData)

    private fun onTermDataLoaded(
        state: DictionaryTabState,
        termList: List<TermUiItem>,
    ): ReducerResult<DictionaryTabState, Effect> = state
        .copy(
            isLoading = false,
            termList = termList
        ) to emptySet()

    private fun onExpandTerm(
        state: DictionaryTabState,
        itemId: Long,
        expand: Boolean
    ): ReducerResult<DictionaryTabState, Effect> = state
        .copy(
            termList = state.termList
                .modifyFiltered(
                    predicate = { it.id == itemId },
                    action = { it.copy(isExpand = expand) },
                )
        ) to emptySet()

    private fun onOpenAddWordWidget(
        state: DictionaryTabState,
        show: Boolean,
        wordValue: String? = null,
        wordId: Long? = null,
    ): ReducerResult<DictionaryTabState, Effect> = state
        .copy(
            addWordDialogState = state.addWordDialogState.copy(
                isOpen = show,
                wordValue = wordValue ?: EMPTY_STRING,
                wordId = wordId,
            )
        ) to emptySet()

    private fun onWordValueChange(
        state: DictionaryTabState,
        newValue: String
    ): ReducerResult<DictionaryTabState, Effect> = state
        .copy(
            addWordDialogState = state.addWordDialogState.copy(wordValue = newValue)
        ) to emptySet()

    private fun onAddWord(
        state: DictionaryTabState,
        value: String
    ): ReducerResult<DictionaryTabState, Effect> =
        state to setOf(
            DatasourceEffect.AddWord(value),
        )

    private fun onChangeWord(
        state: DictionaryTabState,
        wordId: Long,
        value: String
    ): ReducerResult<DictionaryTabState, Effect> = state to setOf(
        DatasourceEffect.ChangeWord(wordId = wordId, value = value),
    )

    private fun onConfirmDeleteWordDialog(
        state: DictionaryTabState,
        isOpen: Boolean,
        wordIds: Set<WordInfo>
    ): ReducerResult<DictionaryTabState, Effect> = state.copy(
        confirmWordDeleteDialogState = state.confirmWordDeleteDialogState.copy(
            isOpen = isOpen,
            wordIds = wordIds
        )
    ) to emptySet()

    private fun onDeleteWord(
        state: DictionaryTabState,
        wordIds: Set<WordInfo>,
    ): ReducerResult<DictionaryTabState, Effect> =
        state to setOf(
            DatasourceEffect.DeleteWord(wordSet = wordIds),
            UiEffect.ShowSnackbar(title = "Слово удалено")
        )
}

typealias StateUpdater = (DictionaryTabState) -> ReducerResult<DictionaryTabState, Effect>

fun combineMsgHandlers(
    stateUpdaters: List<StateUpdater>,
    initState: DictionaryTabState
): ReducerResult<DictionaryTabState, Effect> {
    return stateUpdaters.fold(
        initial = initState to setOf(),
        operation = { (state, accEffects), stateUpdater: StateUpdater ->
            val (midState, effects) = stateUpdater.invoke(state)
            midState to (accEffects + effects)
        }
    )
}