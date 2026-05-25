package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {
    // --- Top bar menu ---
    data object OpenTopBarMenu : Msg
    data object CloseTopBarMenu : Msg

    // --- Delete word ---
    data object OpenDeleteWordDialog : Msg
    data object CloseDeleteWordDialog : Msg
    data class RemoveWord(val wordId: Long) : Msg

    // --- Word edit ---
    data object EnterWordEditMode : Msg
    data class UpdateWordInput(val value: String) : Msg
    data object CommitWordChanges : Msg

    // --- Lexeme ---
    data object CreateLexeme : Msg
    data class OpenDeleteLexemeDialog(val lexemeId: Long) : Msg
    data object CloseDeleteLexemeDialog : Msg
    data class RemoveLexeme(val lexemeId: Long) : Msg

    // --- Translation chip ---
    data class CreateTranslation(val lexemeId: Long) : Msg
    data class UpdateTranslationInput(val lexemeId: Long, val value: String) : Msg
    data class EnterTranslationEditMode(val lexemeId: Long) : Msg
    data class CommitTranslationEdit(val lexemeId: Long) : Msg
    data class RemoveTranslation(val lexemeId: Long) : Msg

    // --- Definition chip ---
    data class CreateDefinition(val lexemeId: Long) : Msg
    data class UpdateDefinitionInput(val lexemeId: Long, val value: String) : Msg
    data class EnterDefinitionEditMode(val lexemeId: Long) : Msg
    data class CommitDefinitionEdit(val lexemeId: Long) : Msg
    data class RemoveDefinition(val lexemeId: Long) : Msg

    // --- Navigation + feedback ---
    data object NavigateBack : Msg
    data object NoOperation : Msg

    // --- Datasource Msg ---
    data class WordLoaded(val word: Term) : Msg
    data object WordNotFound : Msg
    data class RefreshWord(val word: Term) : Msg
    data class RefreshTranslation(val lexemeId: Long, val translation: String?) : Msg
    data class RefreshDefinition(val lexemeId: Long, val definition: String?) : Msg
    data class RefreshLexemeList(val lexemes: List<Lexeme>) : Msg
    data class ShowError(@StringRes val messageRes: Int) : Msg

    // --- Delete events с payload для undo ---
    data class TranslationDeleted(val lexemeId: Long, val removedValue: String) : Msg
    data class DefinitionDeleted(val lexemeId: Long, val removedValue: String) : Msg
    data class LexemeCascadeRemovedWithUndo(
        val lexemeId: Long,
        val removedTranslation: String?,
        val removedDefinition: String?,
    ) : Msg
    data class LexemeRemoved(
        val lexemeId: Long,
        val translation: String?,
        val definition: String?,
    ) : Msg

    // --- Undo Msg ---
    data class UndoRemoveTranslation(val lexemeId: Long, val value: String) : Msg
    data class UndoRemoveDefinition(val lexemeId: Long, val value: String) : Msg
    data class UndoRemoveLexeme(
        val translation: String?,
        val definition: String?,
    ) : Msg
}
