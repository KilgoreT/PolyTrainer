package me.apomazkin.dictionary.list

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionary.model.DictionaryListItem

@Immutable
data class DictionaryListScreenState(
    val isLoading: Boolean = true,
    val dictionaries: List<DictionaryListItem> = listOf(),
    val deleteDialogState: DeleteDialogState = DeleteDialogState(),
)

@Immutable
data class DeleteDialogState(
    val show: Boolean = false,
    val dictionaryId: Long = 0,
    val dictionaryName: String = "",
)

// === Extension Functions ===

fun DictionaryListScreenState.updateDictionaries(
    list: List<DictionaryListItem>
) = copy(dictionaries = list)

fun DictionaryListScreenState.showDeleteDialog(id: Long, name: String) = copy(
    deleteDialogState = DeleteDialogState(
        show = true,
        dictionaryId = id,
        dictionaryName = name,
    ),
)

fun DictionaryListScreenState.hideDeleteDialog() = copy(
    deleteDialogState = DeleteDialogState(),
)

fun DictionaryListScreenState.stopLoading() = copy(isLoading = false)
