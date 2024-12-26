package me.apomazkin.dictionarypicker.state

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionarypicker.entity.DictUiEntity

@Immutable
data class LangPickerState(
    val isLoading: Boolean = true,
    val currentDict: DictUiEntity,
    val availableDictList: List<DictUiEntity> = emptyList(),
    val isDropDownMenuOpen: Boolean = false,
)