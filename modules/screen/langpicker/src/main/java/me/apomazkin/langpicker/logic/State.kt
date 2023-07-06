package me.apomazkin.langpicker.logic

import me.apomazkin.langpicker.entity.PresetLangUi

data class LangPickerState(
    val isLoading: Boolean = true,
    val needClose: Boolean = false,
    val langState: LangState = LangState()
)

data class LangState(
    val langList: List<PresetLangUi> = listOf(),
    val selectedNumericCode: Int? = null,
    val addLangButtonEnable: Boolean = false,
)