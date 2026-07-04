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
     * IS476: nullable — null = словаря нет (все удалены).
     */
    data class CurrentDict(val current: DictUiEntity?) : Msg

    /**
     * Message to change current dictionary.
     */
    data class ChangeDict(val dict: DictUiEntity) : Msg

    /**
     * Messages to expand or collapse Dictionary [DropdownMenu].
     */
    data object DictMenuOn : Msg
    data object DictMenuOff : Msg

    data object OpenDictionaryCreate : Msg

    /**
     * Navigate to per-dictionary components screen (IS481).
     * Payload carries dictionaryId explicitly — reducer does not read state.
     */
    data class OpenPerDictionaryComponents(val dictionaryId: Long) : Msg

    data object Empty : Msg
}