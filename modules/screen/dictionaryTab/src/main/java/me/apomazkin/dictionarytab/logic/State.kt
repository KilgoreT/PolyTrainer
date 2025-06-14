package me.apomazkin.dictionarytab.logic

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarypicker.state.LangPickerState
import me.apomazkin.dictionarytab.entity.LexemeLabel
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.EMPTY_STRING

/**
 * State
 */
@Immutable
data class DictionaryTabState(
        val isLoading: Boolean = true,
        val topBarState: TopBarState = TopBarState(),
        val termList: TermsSource = TermsSource(pattern = ""),
        val termListMap: Map<String, Flow<PagingData<TermUiItem>>> = emptyMap(),
        val addWordDialogState: AddWordDialogState = AddWordDialogState(),
        val snackbarState: SnackbarState = SnackbarState(),
        val confirmWordDeleteDialogState: ConfirmWordDeleteDialogState = ConfirmWordDeleteDialogState(),
)

@Immutable
data class TermsSource(
        val pattern: String,
        val termListFlow: Flow<PagingData<TermUiItem>> = flowOf(),
)

@Immutable
data class TopBarState(
        val isActionMode: Boolean = false,
        val langPickerState: LangPickerState? = null,
        val actionState: Action = Action(),
) {
    @Immutable
    data class Action(
            val selectedTermIds: Set<WordInfo> = emptySet(),
    )
}

@Immutable
data class AddWordDialogState(
        val isOpen: Boolean = false,
        val wordValue: String = EMPTY_STRING,
        val wordId: Long? = null,
)

@Immutable
data class LexemeState(
        val lexemeId: Long? = null,
        val requireSave: Boolean = false,
        val category: LexemeLabel = LexemeLabel.UNDEFINED,
        val definition: EditableTextState = EditableTextState(),
        val translation: String = "",
)

@Immutable
data class EditableTextState(
        val readOnly: Boolean = true,
        val text: String = "",
        val editedText: String = "",
)

@Immutable
data class SnackbarState(
        val title: String = EMPTY_STRING,
        val show: Boolean = false,
)

@Immutable
data class ConfirmWordDeleteDialogState(
        val isOpen: Boolean = false,
        val wordIds: Set<WordInfo> = emptySet(),
)


fun DictionaryTabState.showLoading(): DictionaryTabState =
        this.copy(isLoading = true)

fun DictionaryTabState.hideLoading(): DictionaryTabState =
        this.copy(isLoading = false)


fun DictionaryTabState.applyCurrentDictionary(
        dict: DictUiEntity,
): DictionaryTabState =
        this.copy(
                topBarState = topBarState.copy(
                        langPickerState = LangPickerState(
                                isLoading = false,
                                currentDict = dict,
                        )
                )
        )

fun DictionaryTabState.applyAvailableDictionaries(
        dictList: List<DictUiEntity>,
): DictionaryTabState =
        this.copy(
                topBarState = topBarState.copy(
                        langPickerState = topBarState.langPickerState?.copy(
                                isLoading = false,
                                availableDictList = dictList
                        )
                )
        )

fun DictionaryTabState.showDictMenu(): DictionaryTabState =
        this.copy(
                topBarState = topBarState.copy(
                        langPickerState = topBarState.langPickerState?.copy(
                                isDropDownMenuOpen = true
                        )
                )
        )

fun DictionaryTabState.hideDictMenu(): DictionaryTabState =
        this.copy(
                topBarState = topBarState.copy(
                        langPickerState = topBarState.langPickerState?.copy(
                                isDropDownMenuOpen = false
                        )
                )
        )


/**
 * ###### UPDATE TERMS FLOW ######
 */
fun DictionaryTabState.appendTermsFlow(
        pattern: String,
        termsFlow: Flow<PagingData<TermUiItem>>,
): DictionaryTabState {
    val updatedMap = termListMap + (pattern to termsFlow)
    return this.copy(
            termList = TermsSource(
                    pattern = pattern,
                    termListFlow = termsFlow,
            ),
            termListMap = updatedMap,
    )
}

//TODO kilg 14.06.2025 00:57 надо, чтобы был не эксепшн, а попытка загрузить флоу из базы.
// https://github.com/KilgoreT/PolyTrainer/issues/376
fun DictionaryTabState.toDefaultTermsFlow(): DictionaryTabState {
    return this.copy(
            termList = TermsSource(
                    pattern = "",
                    termListFlow = termListMap[""] ?: throw IllegalStateException(
                            "Default term list flow not found in termListMap."
                    ),
            ),
            termListMap = termListMap.filterKeys { it == "" },
    )
}

fun DictionaryTabState.retainDefaultAndCurrentFlow(
        value: String,
): DictionaryTabState = copy(
        termListMap = termListMap
                .filterKeys { it == "" || it == value }
)

/**
 * ###### ACTION MODE ######
 */
fun DictionaryTabState.showActionMode() = this.copy(
        topBarState = this.topBarState.copy(
                isActionMode = true,
        )
)

fun DictionaryTabState.hideActionMode() = this.copy(
        topBarState = this.topBarState.copy(
                isActionMode = false,
        )
)

fun DictionaryTabState.checkActionMode() = this.copy(
        topBarState = this.topBarState.copy(
                isActionMode = this.topBarState.actionState.selectedTermIds.isNotEmpty(),
        )
)


fun DictionaryTabState.modifySelectedSet(
        targetWord: WordInfo,
): DictionaryTabState {
    val currentSet = topBarState.actionState.selectedTermIds
    val newSet = if (targetWord in currentSet) {
        currentSet - targetWord
    } else {
        currentSet + targetWord
    }
    return this.copy(
            topBarState = this.topBarState.copy(
                    actionState = this.topBarState.actionState.copy(
                            selectedTermIds = newSet
                    )
            )
    )
}

fun DictionaryTabState.clearSelectedSet() = this.copy(
        topBarState = this.topBarState.copy(
                actionState = TopBarState.Action(selectedTermIds = emptySet())
        )
)

fun DictionaryTabState.highlightWord(
        targetWord: WordInfo,
): DictionaryTabState {
    val currentSet = topBarState.actionState.selectedTermIds
    val apply = targetWord in currentSet
    val updatedTermsFlow = termList.termListFlow.map { pagingData ->
        pagingData.map { termUiItem ->
            if (termUiItem.id == targetWord.id) {
                termUiItem.copy(isSelected = apply)
            } else {
                termUiItem
            }
        }
    }
    return this.copy(
            termList = TermsSource(
                    pattern = termList.pattern,
                    termListFlow = updatedTermsFlow,
            ),
    )
}

fun DictionaryTabState.clearHighlighted(): DictionaryTabState {
    val updatedTermsFlow = termList.termListFlow.map { pagingData ->
        pagingData.map { termUiItem ->
            if (termUiItem.isSelected) {
                termUiItem.copy(isSelected = false)
            } else {
                termUiItem
            }
        }
    }
    return this.copy(
            termList = TermsSource(
                    pattern = termList.pattern,
                    termListFlow = updatedTermsFlow,
            ),
    )
}


/**
 * ###### ADD WORD DIALOG ######
 */
fun DictionaryTabState.showAddWordDialog(
        wordValue: String?,
        wordId: Long?,
): DictionaryTabState {
    return this.copy(
            addWordDialogState = AddWordDialogState(
                    isOpen = true,
                    wordValue = wordValue ?: EMPTY_STRING,
                    wordId = wordId,
            )
    )
}

fun DictionaryTabState.hideAddWordDialog(): DictionaryTabState {
    return this.copy(
            addWordDialogState = AddWordDialogState(
                    isOpen = false,
                    wordValue = "",
                    wordId = null,
            )
    )
}

fun DictionaryTabState.updateWordValue(
        value: String,
): DictionaryTabState {
    return this.copy(
            addWordDialogState = addWordDialogState.copy(
                    wordValue = value,
            )
    )
}

fun DictionaryTabState.showConfirmDeleteDialog(
        wordIds: Set<WordInfo>,
): DictionaryTabState {
    return this.copy(
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                    isOpen = true,
                    wordIds = wordIds,
            )
    )
}

fun DictionaryTabState.hideConfirmDeleteDialog(): DictionaryTabState {
    return this.copy(
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                    isOpen = false,
                    wordIds = emptySet(),
            )
    )
}

fun <STATE, EFFECT> STATE.choose(
        check: (STATE) -> Boolean,
        yes: (STATE) -> Pair<STATE, Set<EFFECT>>,
        no: (STATE) -> Pair<STATE, Set<EFFECT>>,
): Pair<STATE, Set<EFFECT>> {
    return if (check.invoke(this)) yes(this) else no(this)
}