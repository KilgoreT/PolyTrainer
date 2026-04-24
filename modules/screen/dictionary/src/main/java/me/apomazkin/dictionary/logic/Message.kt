package me.apomazkin.dictionary.logic

import me.apomazkin.dictionary.entity.PresetDictionaryUi

sealed interface Msg {

    data class ShowDictionaryList(val list: List<PresetDictionaryUi>) : Msg
    data class SelectDictionary(val numericCode: Int) : Msg
    data class SaveDictionary(val numericCode: Int, val dictionaryName: String) : Msg

    object Close : Msg

    /**
     * When no need action after Effect.
     */
    object Empty : Msg

}
