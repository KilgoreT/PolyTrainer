package me.apomazkin.dictionarytab.logic

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionarytab.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.LexemeLabel
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.EMPTY_STRING

/**
 * State
 */
@Immutable
data class VocabularyTabState(
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
    val mainState: Main = Main(),
    val actionState: Action = Action(),
) {
    @Immutable
    data class Main(
        val isLoading: Boolean = true,
        val currentDict: DictUiEntity? = null,
        val availableDictList: List<DictUiEntity> = emptyList(),
        val isDropDownMenuOpen: Boolean = false,
    )

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

internal fun VocabularyTabState.isEmpty(): Boolean =
    this.termList.isEmpty() && !isLoading