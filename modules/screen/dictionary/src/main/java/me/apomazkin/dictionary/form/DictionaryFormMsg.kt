package me.apomazkin.dictionary.form

import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem

sealed interface DictionaryFormMsg {

    // UI messages
    data class NameChanged(val value: String) : DictionaryFormMsg
    data class FlagFilterChanged(val query: String) : DictionaryFormMsg
    data class SelectFlag(val item: CountryFlagItem) : DictionaryFormMsg
    data object Save : DictionaryFormMsg
    data object Back : DictionaryFormMsg
    // IS525: выбор языков
    data class OpenLanguagePicker(val target: LanguageTarget) : DictionaryFormMsg
    data object CloseLanguagePicker : DictionaryFormMsg
    data class LanguageQueryChanged(val query: String) : DictionaryFormMsg
    data class SelectLanguage(val item: LanguageItem) : DictionaryFormMsg

    // Datasource messages
    data class FlagsUpdated(val list: List<CountryFlagItem>) : DictionaryFormMsg
    data class LanguagesLoaded(val all: List<LanguageItem>) : DictionaryFormMsg
    data class DictionaryLoaded(
        val name: String,
        val flag: CountryFlagItem?,
        val learningLanguage: LanguageItem,
        val translationLanguage: LanguageItem,
    ) : DictionaryFormMsg
    data object DictionarySaved : DictionaryFormMsg

    data object Empty : DictionaryFormMsg
}
