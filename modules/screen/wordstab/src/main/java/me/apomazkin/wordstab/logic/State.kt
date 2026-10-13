package me.apomazkin.wordstab.logic

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.entity.LexemeLabel
import me.apomazkin.wordstab.logic.photo.PhotoAddFailure
import me.apomazkin.wordstab.logic.photo.PhotoAddSummary
import me.apomazkin.wordstab.logic.photo.PhotoCard
import me.apomazkin.wordstab.logic.photo.PhotoFormError
import me.apomazkin.wordstab.logic.photo.PhotoNotAdded
import me.apomazkin.wordstab.logic.photo.PhotoSheet
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
 * IS527/IS529/IS532: диалог «слова с фото» явными флагами: выбор источника
 * ([isChoosingSource]) → индикатор с подписью фазы ([phase], WFT-16) →
 * ответ сервера ([words]) и признак загрузки формы ([phase] = FORM) →
 * окно-черновик ([cards], AWP-03), «слов не найдено» ([isEmptyResult],
 * WFT-60) или ошибка ([errorRes], WFT-61, WFT-63; [formError], AWP-24).
 * Закрыт — флаги в умолчаниях. [requestId] — номер запуска (термин
 * «Запуск», WFT-01…03): растёт на каждое открытие выбора и на каждый
 * снимок, переживает закрытие диалога; результат распознавания, ответ
 * сервера, форма и итог записи с чужим id отбрасываются (WFT-62,
 * AWP-24.2). Уходит на сервер как `X-Run-Id`.
 *
 * Окно (Ш2): [dictionaryId] — словарь снимка (AWP-01), [form] — форма
 * словаря (`data: T? = null`), [mapping] — соответствия, [commonGroupId] —
 * общая группа (null — «Все»), [cards] и [notAdded] — список, [summary]
 * — кнопка; [isDirty] — были удаления, правки текста или смены части
 * речи (AWP-19); [isAdding] — запись идёт (AWP-13.4); [addFailure] — В3;
 * вопросы [isConfirmingAdd], [isConfirmingClose]; [sheet] — открытый лист.
 */
@Immutable
data class PhotoWordsState(
        val isOpen: Boolean = false,
        val isChoosingSource: Boolean = false,
        val phase: PhotoPhase = PhotoPhase.IDLE,
        val words: List<PhotoWord> = emptyList(),
        val isEmptyResult: Boolean = false,
        @StringRes val errorRes: Int? = null,
        val requestId: Long = 0L,
        val dictionaryId: Long? = null,
        val form: PhotoDictionaryForm? = null,
        val formError: PhotoFormError? = null,
        val mapping: PhotoMapping = PhotoMapping.NONE,
        val commonGroupId: Long? = null,
        val cards: List<PhotoCard> = emptyList(),
        val notAdded: List<PhotoNotAdded> = emptyList(),
        val summary: PhotoAddSummary = PhotoAddSummary(),
        val isDirty: Boolean = false,
        val isAdding: Boolean = false,
        val addFailure: PhotoAddFailure? = null,
        val isConfirmingAdd: Boolean = false,
        val isConfirmingClose: Boolean = false,
        val isMappingExpanded: Boolean = false,
        val isNotAddedExpanded: Boolean = false,
        val sheet: PhotoSheet? = null,
) {
    /** Индикатор виден, пока идёт распознавание, ожидание ответа или сборка окна (WFT-16, AWP-24). */
    val isBusy: Boolean
        get() = phase != PhotoPhase.IDLE

    /** Окно-черновик собрано: форма есть, ошибок нет (Ш2). */
    val isWindowReady: Boolean
        get() = form != null && formError == null
}

/** Фаза запуска; подпись индикатора выбирается по ней (WFT-16.1, 16.2; AWP-24). */
enum class PhotoPhase {
    IDLE,
    RECOGNIZING,
    LOOKING,
    FORM,
}

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
 * ###### PHOTO WORDS DIALOG (IS527/IS532) ######
 * Шаги диалога — атомы `logic/photo/PhotoStateAtoms`; здесь только предикат.
 */

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