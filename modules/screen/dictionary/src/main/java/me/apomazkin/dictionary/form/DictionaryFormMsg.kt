package me.apomazkin.dictionary.form

import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem

sealed interface DictionaryFormMsg {

    // Form fields
    data class NameChanged(val value: String) : DictionaryFormMsg
    data object ToggleLanguageBound : DictionaryFormMsg
    data object Save : DictionaryFormMsg

    // Language picker
    data object OpenLanguagePicker : DictionaryFormMsg
    data object CloseLanguagePicker : DictionaryFormMsg
    data class LanguageQueryChanged(val query: String) : DictionaryFormMsg
    data class SelectLanguage(val item: LanguageItem) : DictionaryFormMsg

    // Flag
    data class SelectFlag(val item: CountryFlagItem) : DictionaryFormMsg

    // Data (from effects)
    data class LanguagesLoaded(val list: List<LanguageItem>) : DictionaryFormMsg
    data class FlagsLoaded(val list: List<CountryFlagItem>) : DictionaryFormMsg
    data object DictionarySaved : DictionaryFormMsg

    data object Empty : DictionaryFormMsg
}
