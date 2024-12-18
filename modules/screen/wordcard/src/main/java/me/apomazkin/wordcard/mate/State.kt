package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import me.apomazkin.chippicker.CategoryLabel
import me.apomazkin.mate.EMPTY_STRING
import java.util.Date

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val addLexemeBottomState: AddLexemeBottomState = AddLexemeBottomState(),
    val closeScreen: Boolean = false,
    val isLoading: Boolean = true,
    val wordState: WordState = WordState(),
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState()
)

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false
)

@Stable
data class AddLexemeBottomState(
    val show: Boolean = false,
    val isTranslationCheck: Boolean = false,
    val isDefinitionCheck: Boolean = false,
)

@Stable
data class WordState(
    val id: Long = NOT_IN_DB,
    val added: Date? = null,
    val value: String = "",
    val isEditMode: Boolean = false,
    val edited: String = "",
    val showWarningDialog: Boolean = false,
    val deleteButtonEnabled: Boolean = true,
)

@Stable
data class LexemeState(
    val id: Long = NOT_IN_DB,
    val translation: TextValueState? = null,
    val definition: TextValueState? = null,
    val isMenuOpen: Boolean = false,
//    val category: CategoryState = CategoryState(),
)

@Stable
data class CategoryState(
    val origin: CategoryLabel = CategoryLabel.UNDEFINED,
    val edited: CategoryLabel = origin,
)

fun CategoryState.toValue(isEdit: Boolean) = if (isEdit) edited else origin
fun CategoryState.isChanged() = origin != edited

@Stable
data class TextValueState(
    val isEdit: Boolean = true,
    val origin: String = "",
    val edited: String = origin,
)

fun TextValueState.toValue(isEdit: Boolean) = if (isEdit) edited else origin
fun TextValueState.isChanged() = origin != edited

// TODO: вынести в mate
@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)