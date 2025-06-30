package me.apomazkin.stattab.mate

sealed interface Msg {
    data class UpdateStates(
            val wordCount: Int,
            val lexemeCount: Int,
    ): Msg

    data object Empty : Msg
}