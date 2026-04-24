package me.apomazkin.dictionary.logic

import me.apomazkin.dictionary.entity.PresetDictionaryUi

data class DictionaryState(
    val isLoading: Boolean = true,
    val needClose: Boolean = false,
    val dictionarySelectionState: DictionarySelectionState = DictionarySelectionState()
)

data class DictionarySelectionState(
    val dictionaryList: List<PresetDictionaryUi> = listOf(),
    val selectedNumericCode: Int? = null,
    val addDictionaryButtonEnable: Boolean = false,
)
