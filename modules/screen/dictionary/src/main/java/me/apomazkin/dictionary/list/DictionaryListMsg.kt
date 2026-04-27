package me.apomazkin.dictionary.list

import me.apomazkin.dictionary.model.DictionaryListItem

sealed interface DictionaryListMsg {

    // Delete
    data class RequestDelete(val id: Long, val name: String) : DictionaryListMsg
    data object ConfirmDelete : DictionaryListMsg
    data object DismissDelete : DictionaryListMsg

    // Data (from effects)
    data class DictionariesLoaded(val list: List<DictionaryListItem>) : DictionaryListMsg
    data object DictionaryDeleted : DictionaryListMsg

    data object Empty : DictionaryListMsg
}
