package me.apomazkin.createdictionary.logic

import me.apomazkin.createdictionary.entity.PresetDictionaryUi

data class CreateDictionaryState(
    val isLoading: Boolean = true,
    val needClose: Boolean = false,
    val dictionarySelectionState: DictionarySelectionState = DictionarySelectionState()
)

data class DictionarySelectionState(
    val dictionaryList: List<PresetDictionaryUi> = listOf(),
    val selectedNumericCode: Int? = null,
    val addDictionaryButtonEnable: Boolean = false,
)
