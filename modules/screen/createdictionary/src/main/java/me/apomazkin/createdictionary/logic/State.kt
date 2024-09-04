package me.apomazkin.createdictionary.logic

import me.apomazkin.createdictionary.entity.PresetLangUi

data class CreateDictionaryState(
    val isLoading: Boolean = true,
    val needClose: Boolean = false,
    val langState: LangState = LangState()
)

data class LangState(
    val langList: List<PresetLangUi> = listOf(),
    val selectedNumericCode: Int? = null,
    val addLangButtonEnable: Boolean = false,
)