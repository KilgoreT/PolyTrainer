package me.apomazkin.wordcard.mate

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {
    data object LoadingWord : Msg
    data class WordLoaded(val term: Term) : Msg
    data object WordNotFound : Msg

    data object OpenTopBarMenu : Msg
    data object CloseTopBarMenu : Msg
    data object OpenDeleteWordDialog : Msg
    data object CloseDeleteWordDialog : Msg
    data class RemoveWord(val wordId: Long) : Msg

    data object EnterWordEditMode : Msg
    data class UpdateWordInput(val value: String) : Msg
    data object ExitWordEditMode : Msg
    data object CommitWordChanges : Msg

    data object OpenAddLexemeDialog : Msg
    data object CloseAddLexemeDialog : Msg
    data class EnableTranslationCreation(val isAdded: Boolean) : Msg
    data class EnableDefinitionCreation(val isAdded: Boolean) : Msg
    data object CreateLexeme : Msg
    data class RefreshLexeme(val lexeme: Lexeme) : Msg
    data class RemoveLexeme(val lexemeId: Long) : Msg
    data class OpenLexemeMenu(val lexemeId: Long, val isShow: Boolean) : Msg

    data class CreateTranslation(val lexemeId: Long) : Msg
    data class UpdateTranslationInput(val lexemeId: Long, val value: String) : Msg
    data class EnterTranslationEditMode(val lexemeId: Long) : Msg
    data class ExitTranslationEditMode(val lexemeId: Long) : Msg
    data class RefreshTranslation(val lexeme: Lexeme) : Msg
    data class RemoveTranslation(val lexemeId: Long) : Msg

    data class CreateDefinition(val lexemeId: Long) : Msg
    data class UpdateDefinitionInput(val lexemeId: Long, val value: String) : Msg
    data class EnterDefinitionEditMode(val lexemeId: Long) : Msg
    data class ExitDefinitionEditMode(val lexemeId: Long) : Msg
    data class RefreshDefinition(val lexeme: Lexeme) : Msg
    data class RemoveDefinition(val lexemeId: Long) : Msg

    data object NavigateBack : Msg

    data object NoOperation : Msg
}

internal sealed interface UiMsg : Msg {
    /**
     * Message for Snackbar
     * @param text text of Snackbar.
     * @param show variable to reset show status for state.
     */
    data class ShowNotification(val text: String, val show: Boolean) : UiMsg
}