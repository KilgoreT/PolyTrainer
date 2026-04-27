package me.apomazkin.dictionary.form

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem

@Immutable
data class DictionaryFormScreenState(
    val needClose: Boolean = false,
    val editingDictionaryId: Long? = null,
    val name: String = "",
    val isLanguageBound: Boolean = false,
    val selectedLanguage: LanguageItem? = null,
    val availableFlags: List<CountryFlagItem> = listOf(),
    val selectedFlag: CountryFlagItem? = null,
    val saveButtonEnabled: Boolean = false,
    val languagePickerState: LanguagePickerState = LanguagePickerState(),
)

@Immutable
data class LanguagePickerState(
    val show: Boolean = false,
    val query: String = "",
    val languages: List<LanguageItem> = listOf(),
    val filteredLanguages: List<LanguageItem> = listOf(),
)

// === Extension Functions ===

fun DictionaryFormScreenState.updateName(value: String) = copy(
    name = value,
    saveButtonEnabled = value.isNotBlank(),
)

fun DictionaryFormScreenState.toggleLanguageBound() = copy(
    isLanguageBound = !isLanguageBound,
    selectedLanguage = if (isLanguageBound) null else selectedLanguage,
    availableFlags = if (isLanguageBound) listOf() else availableFlags,
    selectedFlag = if (isLanguageBound) null else selectedFlag,
)

fun DictionaryFormScreenState.selectLanguage(language: LanguageItem) = copy(
    selectedLanguage = language,
    languagePickerState = languagePickerState.copy(show = false, query = ""),
)

fun DictionaryFormScreenState.selectFlag(flag: CountryFlagItem) = copy(
    selectedFlag = flag,
)

fun DictionaryFormScreenState.updateFlags(list: List<CountryFlagItem>) = copy(
    availableFlags = list,
    selectedFlag = if (list.size == 1) list.first() else null,
)

fun DictionaryFormScreenState.showLanguagePicker() = copy(
    languagePickerState = languagePickerState.copy(show = true),
)

fun DictionaryFormScreenState.hideLanguagePicker() = copy(
    languagePickerState = languagePickerState.copy(show = false, query = ""),
)

fun DictionaryFormScreenState.filterLanguages(query: String) = copy(
    languagePickerState = languagePickerState.copy(
        query = query,
        filteredLanguages = languagePickerState.languages.filter {
            it.displayName.contains(query, ignoreCase = true)
        },
    ),
)

fun DictionaryFormScreenState.setLanguages(list: List<LanguageItem>) = copy(
    languagePickerState = languagePickerState.copy(
        languages = list,
        filteredLanguages = list,
    ),
)

fun DictionaryFormScreenState.close() = copy(needClose = true)
