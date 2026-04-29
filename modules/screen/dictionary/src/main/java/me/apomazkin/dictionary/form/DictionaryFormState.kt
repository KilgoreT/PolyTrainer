package me.apomazkin.dictionary.form

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionary.model.CountryFlagItem

@Immutable
data class DictionaryFormScreenState(
    val editingDictionaryId: Long? = null,
    val name: String = "",
    val flagFilter: String = "",
    val flags: List<CountryFlagItem> = emptyList(),
    val selectedFlag: CountryFlagItem? = null,
    val saveButtonEnabled: Boolean = false,
)

// === Extension Functions ===

fun DictionaryFormScreenState.updateName(value: String) = copy(
    name = value,
    saveButtonEnabled = value.isNotBlank(),
)

fun DictionaryFormScreenState.selectFlag(flag: CountryFlagItem) = copy(
    selectedFlag = flag,
)

fun DictionaryFormScreenState.deselectFlag() = copy(
    selectedFlag = null,
)

fun DictionaryFormScreenState.updateFlagFilter(query: String) = copy(
    flagFilter = query,
)

fun DictionaryFormScreenState.updateFlags(list: List<CountryFlagItem>) = copy(
    flags = list,
)

fun DictionaryFormScreenState.prefillForEdit(
    name: String,
    flag: CountryFlagItem?,
) = copy(
    name = name,
    selectedFlag = flag,
    saveButtonEnabled = name.isNotBlank(),
)
