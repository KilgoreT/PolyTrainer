package me.apomazkin.wordstab.logic

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.apomazkin.wordstab.entity.LexemeLabel
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.mate.EMPTY_STRING

/**
 * State
 */
@Immutable
data class WordsTabState(
        val isLoading: Boolean = true,
        val hasNoDictionary: Boolean = false,
        val topBarState: TopBarState = TopBarState(),
        val termList: TermsSource = TermsSource(pattern = ""),
        val termListMap: Map<String, Flow<PagingData<TermUiItem>>> = emptyMap(),
        val addWordDialogState: AddWordDialogState = AddWordDialogState(),
        val snackbarState: SnackbarState = SnackbarState(),
        val confirmWordDeleteDialogState: ConfirmWordDeleteDialogState = ConfirmWordDeleteDialogState(),
        val photoWordsState: PhotoWordsState = PhotoWordsState(),
)

@Immutable
data class TermsSource(
        val pattern: String,
        val termListFlow: Flow<PagingData<TermUiItem>> = flowOf(),
)

@Immutable
data class TopBarState(
        val isActionMode: Boolean = false,
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

/**
 * IS527: диалог «слова с фото» — три шага явными флагами: выбор источника
 * ([isChoosingSource]) → распознавание ([isRecognizing]) → список
 * ([words]) или ошибка ([errorRes]). Закрыт — флаги в умолчаниях.
 * [requestId] растёт на каждое открытие выбора и на каждый снимок и
 * переживает закрытие диалога: результат распознавания с чужим id —
 * устаревший, он отбрасывается (эффект mate при закрытии не отменяется).
 */
@Immutable
data class PhotoWordsState(
        val isOpen: Boolean = false,
        val isChoosingSource: Boolean = false,
        val isRecognizing: Boolean = false,
        val words: List<String> = emptyList(),
        @StringRes val errorRes: Int? = null,
        val requestId: Long = 0L,
)

fun WordsTabState.showLoading(): WordsTabState =
        this.copy(isLoading = true)

fun WordsTabState.hideLoading(): WordsTabState =
        this.copy(isLoading = false)

/**
 * IS476: словарь отсутствует — пустое состояние таба.
 * Выставляем флаг и гасим прогресс-индикатор, чтобы UI не висел в Loading.
 */
fun WordsTabState.markNoDictionary(): WordsTabState =
        this.copy(hasNoDictionary = true, isLoading = false)

/**
 * IS476: словарь снова появился — сбрасываем флаг "нет словаря".
 * Возврат isLoading отдельным вызовом showLoading() (см. WordsTabReducer).
 */
fun WordsTabState.markDictionaryPresent(): WordsTabState =
        this.copy(hasNoDictionary = false)

/**
 * ###### UPDATE TERMS FLOW ######
 */
fun WordsTabState.appendTermsFlow(
        pattern: String,
        termsFlow: Flow<PagingData<TermUiItem>>,
): WordsTabState {
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
fun WordsTabState.toDefaultTermsFlow(): WordsTabState {
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

fun WordsTabState.retainDefaultAndCurrentFlow(
        value: String,
): WordsTabState = copy(
        termListMap = termListMap
                .filterKeys { it == "" || it == value }
)

/**
 * ###### ACTION MODE ######
 */
fun WordsTabState.showActionMode() = this.copy(
        topBarState = this.topBarState.copy(
                isActionMode = true,
        )
)

fun WordsTabState.hideActionMode() = this.copy(
        topBarState = this.topBarState.copy(
                isActionMode = false,
        )
)

fun WordsTabState.checkActionMode() = this.copy(
        topBarState = this.topBarState.copy(
                isActionMode = this.topBarState.actionState.selectedTermIds.isNotEmpty(),
        )
)


fun WordsTabState.modifySelectedSet(
        targetWord: WordInfo,
): WordsTabState {
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

fun WordsTabState.clearSelectedSet() = this.copy(
        topBarState = this.topBarState.copy(
                actionState = TopBarState.Action(selectedTermIds = emptySet())
        )
)

fun WordsTabState.highlightWord(
        targetWord: WordInfo,
): WordsTabState {
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

fun WordsTabState.clearHighlighted(): WordsTabState {
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
fun WordsTabState.showAddWordDialog(
        wordValue: String?,
        wordId: Long?,
): WordsTabState {
    return this.copy(
            addWordDialogState = AddWordDialogState(
                    isOpen = true,
                    wordValue = wordValue ?: EMPTY_STRING,
                    wordId = wordId,
            )
    )
}

fun WordsTabState.hideAddWordDialog(): WordsTabState {
    return this.copy(
            addWordDialogState = AddWordDialogState(
                    isOpen = false,
                    wordValue = "",
                    wordId = null,
            )
    )
}

fun WordsTabState.updateWordValue(
        value: String,
): WordsTabState {
    return this.copy(
            addWordDialogState = addWordDialogState.copy(
                    wordValue = value,
            )
    )
}

fun WordsTabState.showConfirmDeleteDialog(
        wordIds: Set<WordInfo>,
): WordsTabState {
    return this.copy(
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                    isOpen = true,
                    wordIds = wordIds,
            )
    )
}

fun WordsTabState.hideConfirmDeleteDialog(): WordsTabState {
    return this.copy(
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                    isOpen = false,
                    wordIds = emptySet(),
            )
    )
}

/**
 * ###### PHOTO WORDS DIALOG (IS527) ######
 */
/** Новый диалог — новый id: результат прежнего снимка на шаг выбора не попадёт. */
fun WordsTabState.showPhotoSourceChooser(): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                isChoosingSource = true,
                requestId = photoWordsState.requestId + 1,
        )
)

/** Новый снимок — новый id: результаты прежних распознаваний устарели. */
fun WordsTabState.startRecognizing(): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                isRecognizing = true,
                requestId = photoWordsState.requestId + 1,
        )
)

fun WordsTabState.showRecognizedWords(
        words: List<String>,
): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                words = words,
                requestId = photoWordsState.requestId,
        )
)

fun WordsTabState.showRecognitionError(
        @StringRes errorRes: Int,
): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                isOpen = true,
                errorRes = errorRes,
                requestId = photoWordsState.requestId,
        )
)

fun WordsTabState.hidePhotoWords(): WordsTabState = this.copy(
        photoWordsState = PhotoWordsState(
                requestId = photoWordsState.requestId,
        )
)

/** Результат относится к текущему открытому диалогу, а не к прошлому снимку. */
fun WordsTabState.isCurrentPhotoRequest(
        requestId: Long,
): Boolean = photoWordsState.isOpen && photoWordsState.requestId == requestId

fun <STATE, EFFECT> STATE.choose(
        check: (STATE) -> Boolean,
        yes: (STATE) -> Pair<STATE, Set<EFFECT>>,
        no: (STATE) -> Pair<STATE, Set<EFFECT>>,
): Pair<STATE, Set<EFFECT>> {
    return if (check.invoke(this)) yes(this) else no(this)
}