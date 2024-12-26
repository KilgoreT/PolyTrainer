package me.apomazkin.dictionarytab.logic

import androidx.compose.runtime.Immutable
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
    val termList: List<TermUiItem> = emptyList(),
    val addWordDialogState: AddWordDialogState = AddWordDialogState(),
    val snackbarState: SnackbarState = SnackbarState(),
    val confirmWordDeleteDialogState: ConfirmWordDeleteDialogState = ConfirmWordDeleteDialogState(),
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
    val editedText: String = ""
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

internal fun DictionaryTabState.isEmpty(): Boolean =
    this.termList.isEmpty() && !isLoading