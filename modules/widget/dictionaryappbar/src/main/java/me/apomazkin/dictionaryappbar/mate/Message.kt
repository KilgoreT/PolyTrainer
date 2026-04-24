package me.apomazkin.dictionaryappbar.mate

import androidx.compose.material3.DropdownMenu
import me.apomazkin.dictionarypicker.entity.DictUiEntity


sealed interface Msg {

    /**
     * Message to setup available dictionary list.
     */
    data class AvailableDict(val list: List<DictUiEntity>) : Msg

    /**
     * Message to setup current dictionary.
     */
    data class CurrentDict(val current: DictUiEntity) : Msg

    /**
     * Message to change current dictionary.
     */
    data class ChangeDict(val dict: DictUiEntity) : Msg

    /**
     * Messages to expand or collapse Dictionary [DropdownMenu].
     */
    data object DictMenuOn : Msg
    data object DictMenuOff : Msg

    data object Empty : Msg
}