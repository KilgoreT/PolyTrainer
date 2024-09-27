package me.apomazkin.vocabulary.logic

import androidx.compose.runtime.Immutable
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.vocabulary.entity.DictUiEntity
import me.apomazkin.vocabulary.entity.LexemeLabel
import me.apomazkin.vocabulary.entity.TermUiItem

/**
 * State
 */
@Immutable
data class VocabularyTabState(
    val isLoading: Boolean = true,
    val topBarActionState: TopBarActionState = TopBarActionState(),
    val termList: List<TermUiItem> = emptyList(),
    val addWordDialogState: AddWordDialogState = AddWordDialogState(),
    val wordDetailDialogState: WordDetailDialogState = WordDetailDialogState(),
    val snackbarState: SnackbarState = SnackbarState()
)

@Immutable
data class TopBarActionState(
    val isLoading: Boolean = true,
    val currentDict: DictUiEntity? = null,
    val availableDictList: List<DictUiEntity> = emptyList(),
    val isDropDownMenuOpen: Boolean = false,
)

@Immutable
data class AddWordDialogState(
    val isAddWordWidgetOpen: Boolean = false,
    val addWordValue: String = EMPTY_STRING,
)

@Immutable
data class WordDetailDialogState(
    val isOpen: Boolean = false,
    val wordId: Long = -1,
    val word: String = "",
    val lexemeList: List<LexemeState> = listOf(LexemeState())
)

fun newLexemeState(): LexemeState = LexemeState(
    requireSave = true,
    definition = EditableTextState(
        readOnly = false
    )
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

internal fun VocabularyTabState.isEmpty(): Boolean =
    this.termList.isEmpty() && !isLoading