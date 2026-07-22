package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValue
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.wordcard.deps.AvailableComponents
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

    // --- Component value (generic) ---
    data class CreateComponentValue(val lexemeId: Long, val typeId: ComponentTypeId) : Msg
    data class UpdateComponentValueInput(val lexemeId: Long, val key: ComponentValueKey, val value: String) : Msg
    data class EnterComponentValueEditMode(val lexemeId: Long, val key: ComponentValueKey) : Msg
    data class CommitComponentValueEdit(val lexemeId: Long, val key: ComponentValueKey) : Msg
    data class RemoveComponentValueRequested(val lexemeId: Long, val key: ComponentValueKey) : Msg

    /**
     * IS486: выбор опции CHOICE-компонента (пикер-диалог) — коммитит сразу, без edit-режима.
     * Reducer сам решает: существующее значение типа → UpdateValue, нет → AddValue.
     */
    data class SelectComponentOption(val lexemeId: Long, val typeId: ComponentTypeId, val optionId: Long) : Msg

    // --- Component types stream ---
    data class ComponentTypesLoaded(val available: AvailableComponents) : Msg
    data class ComponentTypesLoadFailed(val error: Throwable) : Msg
    data object RetryLoadComponentTypes : Msg

    // --- Datasource events ---
    data class WordLoaded(val word: Term) : Msg
    data object WordNotFound : Msg
    data class RefreshWord(val word: Term) : Msg
    data class RefreshLexemeComponents(val lexemeId: Long, val components: List<ComponentValue>) : Msg
    data class ComponentValueInserted(
        val lexemeId: Long,
        val pristineKey: Long,
        val newCvId: ComponentValueId,
    ) : Msg
    data class LexemeDraftPromoted(val newLexeme: Lexeme, val anchorPristineKey: Long) : Msg

    // --- Delete / undo ---
    // IS486 фаза 3: LexemeCascadeRemoved упразднён (деградация в черновик, spec §9.1).
    data class LexemeRemoved(val removedLexeme: Lexeme) : Msg
    data class UndoRestoreLexeme(val lexeme: Lexeme) : Msg
    data class RestoreLexemeFailed(val snapshot: Lexeme) : Msg

    // --- Errors / nav ---
    data class OperationFailed(@StringRes val messageRes: Int) : Msg
    data object NavigateBack : Msg
    data object NoOperation : Msg
}
