package me.apomazkin.wordcard.mate

import me.apomazkin.chippicker.CategoryLabel
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {
    object TermLoading : Msg
    data class TermLoaded(val term: Term) : Msg
    object ShowDeleteWordDialog : Msg
    object HideDeleteWordDialog : Msg
    data class DeleteWord(val wordId: Long) : Msg
    data class ChangeWordValue(val value: String) : Msg
    object OpenEditWord : Msg
    object CloseEditWord : Msg
    object SaveWordValue : Msg
    object AddLexeme : Msg
    data class DeleteLexeme(val lexemeId: Long) : Msg
    data class EditLexeme(val lexemeId: Long?) : Msg
    data class ResetLexeme(val lexemeId: Long) : Msg
    data class SaveLexeme(val lexemeId: Long) : Msg
    data class LexicalCategoryChange(val lexemeId: Long, val category: CategoryLabel) : Msg
    data class LexicalCategoryReset(val lexemeId: Long) : Msg
    data class DefinitionChange(val lexemeId: Long, val value: String) : Msg

    object CloseScreen : Msg

    object Empty : Msg
}

internal sealed interface UiMsg : Msg {
    /**
     * Message for Snackbar
     * @param text text of Snackbar.
     * @param show variable to reset show status for state.
     */
    data class Snackbar(val text: String, val show: Boolean) : UiMsg
}