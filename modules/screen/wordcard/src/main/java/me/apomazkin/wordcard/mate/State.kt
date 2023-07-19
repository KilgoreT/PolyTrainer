package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import me.apomazkin.chippicker.CategoryLabel
import me.apomazkin.mate.EMPTY_STRING

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val closeScreen: Boolean = false,
    val isLoading: Boolean = true,
    val canAddLexeme: Boolean = true,
    val wordState: WordState = WordState(),
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState()
)

@Stable
data class WordState(
    val id: Long = NOT_IN_DB,
    val value: String = "",
    val isEdit: Boolean = false,
    val edited: String = "",
    val showWarningDialog: Boolean = false,
    val deleteButtonEnabled: Boolean = true,
)

@Stable
data class LexemeState(
    val isEdit: Boolean = true,
    val id: Long = NOT_IN_DB,
    val category: CategoryState = CategoryState(),
    val definition: DefinitionState = DefinitionState(),
)

@Stable
data class CategoryState(
    val origin: CategoryLabel = CategoryLabel.UNDEFINED,
    val edited: CategoryLabel = origin,
)

fun CategoryState.toValue(isEdit: Boolean) = if (isEdit) edited else origin
fun CategoryState.isChanged() = origin != edited

@Stable
data class DefinitionState(
    val origin: String = "",
    val edited: String = origin,
)

fun DefinitionState.toValue(isEdit: Boolean) = if (isEdit) edited else origin
fun DefinitionState.isChanged() = origin != edited

// TODO: вынести в mate
@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)