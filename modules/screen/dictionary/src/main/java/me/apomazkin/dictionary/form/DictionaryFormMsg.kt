package me.apomazkin.dictionary.form

import me.apomazkin.dictionary.model.CountryFlagItem

sealed interface DictionaryFormMsg {

    // UI messages
    data class NameChanged(val value: String) : DictionaryFormMsg
    data class FlagFilterChanged(val query: String) : DictionaryFormMsg
    data class SelectFlag(val item: CountryFlagItem) : DictionaryFormMsg
    data object Save : DictionaryFormMsg
    data object Back : DictionaryFormMsg

    // Datasource messages
    data class FlagsUpdated(val list: List<CountryFlagItem>) : DictionaryFormMsg
    data class DictionaryLoaded(
        val name: String,
        val flag: CountryFlagItem?,
    ) : DictionaryFormMsg
    data object DictionarySaved : DictionaryFormMsg

    data object Empty : DictionaryFormMsg
}
