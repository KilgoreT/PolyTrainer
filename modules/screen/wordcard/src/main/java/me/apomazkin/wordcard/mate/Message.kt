package me.apomazkin.wordcard.mate

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {
    data object TermLoading : Msg
    data class TermLoaded(val term: Term) : Msg
    data object TermNotLoaded : Msg

    data object ShowDropdownMenu : Msg
    data object HideDropdownMenu : Msg
    data object ShowDeleteWordDialog : Msg
    data object HideDeleteWordDialog : Msg
    data class DeleteWord(val wordId: Long) : Msg

    data object OpenEditWord : Msg
    data class ChangeWordValue(val value: String) : Msg
    data object CloseEditWord : Msg
    data object SaveWordValue : Msg

    data object ShowAddLexemeBottom : Msg
    data object HideAddLexemeBottom : Msg
    data class AddLexemeBottomTranslation(val isAdded: Boolean) : Msg
    data class AddLexemeBottomDefinition(val isAdded: Boolean) : Msg
    data object AddLexeme : Msg
    data class LexemeUpdate(val lexeme: Lexeme) : Msg
    data class DeleteLexeme(val lexemeId: Long) : Msg
    data class ShowLexemeDropDown(val lexemeId: Long, val isShow: Boolean) : Msg

    data class AppendTranslation(val lexemeId: Long) : Msg
    data class TranslationTextChange(val lexemeId: Long, val value: String) : Msg
    data class TranslationOpenEdit(val lexemeId: Long) : Msg
    data class TranslationEndEdit(val lexemeId: Long) : Msg
    data class TranslationUpdate(val lexeme: Lexeme) : Msg
    data class DeleteTranslation(val lexemeId: Long) : Msg

    data class AppendDefinition(val lexemeId: Long) : Msg
    data class DefinitionTextChange(val lexemeId: Long, val value: String) : Msg
    data class DefinitionOpenEdit(val lexemeId: Long) : Msg
    data class DefinitionEndEdit(val lexemeId: Long) : Msg
    data class DefinitionUpdate(val lexeme: Lexeme) : Msg
    data class DeleteDefinition(val lexemeId: Long) : Msg

    data object CloseScreen : Msg

    data object Empty : Msg
}

internal sealed interface UiMsg : Msg {
    /**
     * Message for Snackbar
     * @param text text of Snackbar.
     * @param show variable to reset show status for state.
     */
    data class Snackbar(val text: String, val show: Boolean) : UiMsg
}