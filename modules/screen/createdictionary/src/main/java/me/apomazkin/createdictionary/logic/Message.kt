package me.apomazkin.createdictionary.logic

import me.apomazkin.createdictionary.entity.PresetLangUi

sealed interface Msg {

    data class ShowLangList(val list: List<PresetLangUi>) : Msg
    data class SelectLang(val numericCode: Int) : Msg
    data class SaveLang(val numericCode: Int, val langName: String) : Msg

    object Close : Msg

    /**
     * When no need action after Effect.
     */
    object Empty : Msg

}