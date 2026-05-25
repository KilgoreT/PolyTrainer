package me.apomazkin.wordcard.mate

import me.apomazkin.core_resources.R
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.ReducerResult

class WordCardReducer : MateReducer<WordCardState, Msg, Effect> {

    override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        if (state.isPendingDbOp && message.isGuardedByPending()) {
            return state to emptySet()
        }
        return reduceImpl(state, message)
    }

    private fun reduceImpl(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        return when (message) {
            // ===== Top bar menu =====
            is Msg.OpenTopBarMenu -> state.showMenu() to emptySet()

            is Msg.CloseTopBarMenu ->
                if (!state.topBarState.isMenuOpen) state to emptySet()
                else state.hideMenu() to emptySet()

            // ===== Delete word =====
            is Msg.OpenDeleteWordDialog -> {
                if (state.wordState !is WordState.Loaded) state to emptySet()
                else state.showWordWarningDialog() to emptySet()
            }

            is Msg.CloseDeleteWordDialog -> state.hideWordWarningDialog() to emptySet()

            is Msg.RemoveWord -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null || loaded.id != message.wordId) {
                    state to emptySet()
                } else {
                    state.copy(isPendingDbOp = true)
                        .hideWordWarningDialog()
                        .hideMenu() to setOf(DatasourceEffect.RemoveWord(wordId = message.wordId))
                }
            }

            // ===== Word edit =====
            is Msg.EnterWordEditMode -> {
                if (state.wordState !is WordState.Loaded) state to emptySet()
                else {
                    val (closedState, commitEffects) = state.commitAndCloseAllEdits()
                    closedState.enableWordEdit() to commitEffects
                }
            }

            is Msg.UpdateWordInput -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null || !loaded.isEditMode) state to emptySet()
                else state.updateWordEdited(message.value) to emptySet()
            }

            is Msg.CommitWordChanges -> {
                val loaded = state.wordState as? WordState.Loaded
                when {
                    loaded == null -> state to emptySet()
                    loaded.edited.isBlank() -> state to emptySet()
                    else -> state.copy(isPendingDbOp = true)
                        .disableWordEdit() to setOf(
                        DatasourceEffect.UpdateWord(
                            wordId = loaded.id,
                            value = loaded.edited,
                        )
                    )
                }
            }

            // ===== Lexeme =====
            is Msg.CreateLexeme -> {
                val loaded = state.wordState as? WordState.Loaded
                when {
                    loaded == null -> state to emptySet()
                    state.isCreatingLexeme -> state to emptySet()
                    else -> {
                        val (closed, commitEffects) = state.commitAndCloseAllEdits()
                        closed.copy(
                            lexemeList = listOf(
                                LexemeState(
                                    id = NOT_IN_DB,
                                    translation = null,
                                    definition = null,
                                ),
                            ) + closed.lexemeList,
                        ) to commitEffects
                    }
                }
            }

            is Msg.OpenDeleteLexemeDialog -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                when {
                    lex == null -> state to emptySet()
                    // Пустая NOT_IN_DB-лексема — удаляем сразу без confirm.
                    lex.id == NOT_IN_DB && lex.translation == null && lex.definition == null ->
                        state.removeLexeme(message.lexemeId) to emptySet()
                    else ->
                        state.copy(lexemeIdPendingDelete = message.lexemeId) to emptySet()
                }
            }

            is Msg.CloseDeleteLexemeDialog -> {
                state.copy(lexemeIdPendingDelete = null) to emptySet()
            }

            is Msg.RemoveLexeme -> {
                val loaded = state.wordState as? WordState.Loaded
                val cleared = state.copy(lexemeIdPendingDelete = null)
                when {
                    loaded == null -> cleared to emptySet()
                    message.lexemeId == NOT_IN_DB ->
                        cleared.removeLexeme(NOT_IN_DB) to emptySet()
                    else ->
                        cleared.copy(isPendingDbOp = true) to setOf(
                            DatasourceEffect.RemoveLexeme(
                                wordId = loaded.id,
                                lexemeId = message.lexemeId,
                            )
                        )
                }
            }

            // ===== Translation chip =====
            is Msg.CreateTranslation -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                if (lex == null || lex.translation != null) state to emptySet()
                else {
                    val (closed, commitEffects) = state.commitAndCloseAllEdits()
                    closed.createLexemeTranslation(message.lexemeId) to commitEffects
                }
            }

            is Msg.UpdateTranslationInput -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                val t = lex?.translation
                if (t == null || !t.isEdit) state to emptySet()
                else state.updateLexemeTranslationText(message.lexemeId, message.value) to emptySet()
            }

            is Msg.EnterTranslationEditMode -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                if (lex?.translation == null) state to emptySet()
                else {
                    val (closed, commitEffects) = state.commitAndCloseAllEdits()
                    closed.enableLexemeTranslationEdit(message.lexemeId) to commitEffects
                }
            }

            is Msg.CommitTranslationEdit -> commitTranslationEdit(state, message.lexemeId)

            is Msg.RemoveTranslation -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                when {
                    lex == null -> state to emptySet()
                    lex.id == NOT_IN_DB -> {
                        val nullified = state.updateLexeme(message.lexemeId) {
                            it.copy(translation = null)
                        }
                        val after = nullified.lexemeList.first { it.id == message.lexemeId }
                        if (after.translation == null && after.definition == null) {
                            nullified.removeLexeme(message.lexemeId) to emptySet()
                        } else {
                            nullified to emptySet()
                        }
                    }
                    // Translation ещё не сохранён в БД (пустой chip у real-лексемы) —
                    // локальный nullify без эффекта.
                    lex.translation?.origin?.isEmpty() == true ->
                        state.updateLexeme(message.lexemeId) {
                            it.copy(translation = null)
                        } to emptySet()
                    else -> state.copy(isPendingDbOp = true) to setOf(
                        DatasourceEffect.RemoveTranslation(
                            lexemeId = message.lexemeId,
                            currentValue = lex.translation?.origin.orEmpty(),
                        )
                    )
                }
            }

            // ===== Definition chip (зеркально Translation) =====
            is Msg.CreateDefinition -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                if (lex == null || lex.definition != null) state to emptySet()
                else {
                    val (closed, commitEffects) = state.commitAndCloseAllEdits()
                    closed.createLexemeDefinition(message.lexemeId) to commitEffects
                }
            }

            is Msg.UpdateDefinitionInput -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                val d = lex?.definition
                if (d == null || !d.isEdit) state to emptySet()
                else state.updateLexemeDefinitionText(message.lexemeId, message.value) to emptySet()
            }

            is Msg.EnterDefinitionEditMode -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                if (lex?.definition == null) state to emptySet()
                else {
                    val (closed, commitEffects) = state.commitAndCloseAllEdits()
                    closed.enableLexemeDefinitionEdit(message.lexemeId) to commitEffects
                }
            }

            is Msg.CommitDefinitionEdit -> commitDefinitionEdit(state, message.lexemeId)

            is Msg.RemoveDefinition -> {
                val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                when {
                    lex == null -> state to emptySet()
                    lex.id == NOT_IN_DB -> {
                        val nullified = state.updateLexeme(message.lexemeId) {
                            it.copy(definition = null)
                        }
                        val after = nullified.lexemeList.first { it.id == message.lexemeId }
                        if (after.translation == null && after.definition == null) {
                            nullified.removeLexeme(message.lexemeId) to emptySet()
                        } else {
                            nullified to emptySet()
                        }
                    }
                    // Definition ещё не сохранён в БД (пустой chip у real-лексемы) —
                    // локальный nullify без эффекта.
                    lex.definition?.origin?.isEmpty() == true ->
                        state.updateLexeme(message.lexemeId) {
                            it.copy(definition = null)
                        } to emptySet()
                    else -> state.copy(isPendingDbOp = true) to setOf(
                        DatasourceEffect.RemoveDefinition(
                            lexemeId = message.lexemeId,
                            currentValue = lex.definition?.origin.orEmpty(),
                        )
                    )
                }
            }

            // ===== Navigation / feedback =====
            is Msg.NavigateBack ->
                state.copy(isPendingDbOp = false) to setOf(NavigationEffect.Back)

            is Msg.NoOperation -> state to emptySet()

            // ===== Datasource Msg =====
            is Msg.WordLoaded -> {
                val w = message.word
                state.copy(
                    isLoading = false,
                    isPendingDbOp = false,
                    wordState = WordState.Loaded(
                        id = w.wordId.id,
                        added = w.addedDate,
                        value = w.word.value,
                    ),
                    lexemeList = w.lexemeList.map { it.toLexemeState() },
                ) to emptySet()
            }

            is Msg.WordNotFound ->
                state.copy(isLoading = false, isPendingDbOp = false) to
                        setOf(NavigationEffect.Back)

            is Msg.RefreshWord -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null) {
                    state.copy(isPendingDbOp = false) to emptySet()
                } else {
                    state.copy(
                        isPendingDbOp = false,
                        wordState = loaded.copy(
                            value = message.word.word.value,
                            isEditMode = false,
                            edited = "",
                        ),
                    ) to emptySet()
                }
            }

            is Msg.RefreshTranslation -> refreshTranslation(state, message.lexemeId, message.translation)
            is Msg.RefreshDefinition -> refreshDefinition(state, message.lexemeId, message.definition)

            is Msg.RefreshLexemeList -> {
                val mapped = message.lexemes.map { it.toLexemeState() }
                val keepLocal = state.lexemeList.firstOrNull { it.id == NOT_IN_DB }
                val newList = if (keepLocal != null && mapped.none { it.id == NOT_IN_DB }) {
                    listOf(keepLocal) + mapped
                } else {
                    mapped
                }
                state.copy(isPendingDbOp = false, lexemeList = newList) to emptySet()
            }

            is Msg.ShowError ->
                state.copy(isPendingDbOp = false) to setOf(UiEffect.ShowErrorSnackbar(message.messageRes))

            // ===== Delete events with undo =====
            is Msg.TranslationDeleted -> {
                // Translation удалён из существующей лексемы. State: убрать translation,
                // снять pending. Effect: показать snackbar c undo.
                state.updateLexeme(message.lexemeId) { it.copy(translation = null) }
                    .copy(isPendingDbOp = false) to setOf(
                    UiEffect.ShowSnackbarWithUndo(
                        messageRes = R.string.word_card_snackbar_translation_deleted,
                        actionLabelRes = R.string.word_card_snackbar_undo,
                        undoMsg = Msg.UndoRemoveTranslation(
                            lexemeId = message.lexemeId,
                            value = message.removedValue,
                        ),
                    )
                )
            }

            is Msg.DefinitionDeleted -> {
                state.updateLexeme(message.lexemeId) { it.copy(definition = null) }
                    .copy(isPendingDbOp = false) to setOf(
                    UiEffect.ShowSnackbarWithUndo(
                        messageRes = R.string.word_card_snackbar_definition_deleted,
                        actionLabelRes = R.string.word_card_snackbar_undo,
                        undoMsg = Msg.UndoRemoveDefinition(
                            lexemeId = message.lexemeId,
                            value = message.removedValue,
                        ),
                    )
                )
            }

            is Msg.LexemeCascadeRemovedWithUndo -> {
                // Cascade: лексема удалена из БД, в UI — NOT_IN_DB-черновик. Текст snackbar
                // зависит от того, какая субсущность была удалена пользователем.
                val newState = state.copy(isPendingDbOp = false)
                    .updateLexeme(message.lexemeId) {
                        it.copy(
                            id = NOT_IN_DB,
                            translation = null,
                            definition = null,
                        )
                    }
                val effect = when {
                    message.removedTranslation != null -> UiEffect.ShowSnackbarWithUndo(
                        messageRes = R.string.word_card_snackbar_translation_deleted,
                        actionLabelRes = R.string.word_card_snackbar_undo,
                        undoMsg = Msg.UndoRemoveTranslation(
                            lexemeId = NOT_IN_DB,
                            value = message.removedTranslation,
                        ),
                    )
                    message.removedDefinition != null -> UiEffect.ShowSnackbarWithUndo(
                        messageRes = R.string.word_card_snackbar_definition_deleted,
                        actionLabelRes = R.string.word_card_snackbar_undo,
                        undoMsg = Msg.UndoRemoveDefinition(
                            lexemeId = NOT_IN_DB,
                            value = message.removedDefinition,
                        ),
                    )
                    else -> null
                }
                newState to setOfNotNull(effect)
            }

            is Msg.LexemeRemoved -> {
                // Full-delete лексемы через DeleteLexemeButton. State: убрать лексему,
                // снять pending. Если есть snapshot translation/definition — snackbar с undo.
                val cleared = state.copy(isPendingDbOp = false)
                    .removeLexeme(message.lexemeId)
                val hasSnapshot = message.translation != null || message.definition != null
                val effect = if (hasSnapshot) {
                    UiEffect.ShowSnackbarWithUndo(
                        messageRes = R.string.word_card_snackbar_lexeme_deleted,
                        actionLabelRes = R.string.word_card_snackbar_undo,
                        undoMsg = Msg.UndoRemoveLexeme(
                            translation = message.translation,
                            definition = message.definition,
                        ),
                    )
                } else null
                cleared to setOfNotNull(effect)
            }

            // ===== Undo handlers (IS479) =====
            is Msg.UndoRemoveTranslation -> {
                val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
                // lexemeId == NOT_IN_DB → re-INSERT новой лексемы (cascade-случай).
                // Иначе → restore translation в существующей.
                val effectLexemeId: Long? = if (message.lexemeId == NOT_IN_DB) null else message.lexemeId
                state.copy(isPendingDbOp = true) to setOf(
                    DatasourceEffect.UpdateLexemeTranslation(
                        wordId = loaded.id,
                        lexemeId = effectLexemeId,
                        translation = message.value,
                    )
                )
            }

            is Msg.UndoRemoveDefinition -> {
                val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
                val effectLexemeId: Long? = if (message.lexemeId == NOT_IN_DB) null else message.lexemeId
                state.copy(isPendingDbOp = true) to setOf(
                    DatasourceEffect.UpdateLexemeDefinition(
                        wordId = loaded.id,
                        lexemeId = effectLexemeId,
                        definition = message.value,
                    )
                )
            }

            is Msg.UndoRemoveLexeme -> {
                val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
                if (message.translation == null && message.definition == null) {
                    state to emptySet()
                } else {
                    state.copy(isPendingDbOp = true) to setOf(
                        DatasourceEffect.RestoreLexeme(
                            wordId = loaded.id,
                            translation = message.translation,
                            definition = message.definition,
                        )
                    )
                }
            }
        }
    }

    // ===== Helpers =====

    private fun commitTranslationEdit(
        state: WordCardState,
        lexemeId: Long,
    ): ReducerResult<WordCardState, Effect> {
        val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
        val lex = state.lexemeList.firstOrNull { it.id == lexemeId }
            ?: return state to emptySet()
        val t = lex.translation
        if (t == null || !t.isEdit) return state to emptySet()
        val edited = t.edited
        val origin = t.origin
        return when {
            edited.isBlank() && origin.isEmpty() -> {
                val nullified = state.updateLexeme(lexemeId) { it.copy(translation = null) }
                val after = nullified.lexemeList.first { it.id == lexemeId }
                val final = if (lex.id == NOT_IN_DB && after.translation == null && after.definition == null) {
                    nullified.removeLexeme(lexemeId)
                } else nullified
                final to emptySet()
            }
            edited.isBlank() && origin.isNotEmpty() -> {
                state.copy(isPendingDbOp = true)
                    .updateLexeme(lexemeId) {
                        it.copy(translation = it.translation?.copy(isEdit = false, edited = ""))
                    } to setOf(
                    DatasourceEffect.RemoveTranslation(
                        lexemeId = lexemeId,
                        currentValue = origin,
                    )
                )
            }
            edited == origin -> {
                state.updateLexeme(lexemeId) {
                    it.copy(translation = it.translation?.copy(isEdit = false, edited = ""))
                } to emptySet()
            }
            else -> {
                val effectLexemeId: Long? = if (lex.id == NOT_IN_DB) null else lex.id
                state.copy(isPendingDbOp = true)
                    .updateLexeme(lexemeId) {
                        it.copy(translation = it.translation?.copy(isEdit = false, edited = ""))
                    } to setOf(
                    DatasourceEffect.UpdateLexemeTranslation(
                        wordId = loaded.id,
                        lexemeId = effectLexemeId,
                        translation = edited,
                    )
                )
            }
        }
    }

    private fun commitDefinitionEdit(
        state: WordCardState,
        lexemeId: Long,
    ): ReducerResult<WordCardState, Effect> {
        val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
        val lex = state.lexemeList.firstOrNull { it.id == lexemeId }
            ?: return state to emptySet()
        val d = lex.definition
        if (d == null || !d.isEdit) return state to emptySet()
        val edited = d.edited
        val origin = d.origin
        return when {
            edited.isBlank() && origin.isEmpty() -> {
                val nullified = state.updateLexeme(lexemeId) { it.copy(definition = null) }
                val after = nullified.lexemeList.first { it.id == lexemeId }
                val final = if (lex.id == NOT_IN_DB && after.translation == null && after.definition == null) {
                    nullified.removeLexeme(lexemeId)
                } else nullified
                final to emptySet()
            }
            edited.isBlank() && origin.isNotEmpty() -> {
                state.copy(isPendingDbOp = true)
                    .updateLexeme(lexemeId) {
                        it.copy(definition = it.definition?.copy(isEdit = false, edited = ""))
                    } to setOf(
                    DatasourceEffect.RemoveDefinition(
                        lexemeId = lexemeId,
                        currentValue = origin,
                    )
                )
            }
            edited == origin -> {
                state.updateLexeme(lexemeId) {
                    it.copy(definition = it.definition?.copy(isEdit = false, edited = ""))
                } to emptySet()
            }
            else -> {
                val effectLexemeId: Long? = if (lex.id == NOT_IN_DB) null else lex.id
                state.copy(isPendingDbOp = true)
                    .updateLexeme(lexemeId) {
                        it.copy(definition = it.definition?.copy(isEdit = false, edited = ""))
                    } to setOf(
                    DatasourceEffect.UpdateLexemeDefinition(
                        wordId = loaded.id,
                        lexemeId = effectLexemeId,
                        definition = edited,
                    )
                )
            }
        }
    }

    private fun refreshTranslation(
        state: WordCardState,
        lexemeId: Long,
        translation: String?,
    ): ReducerResult<WordCardState, Effect> {
        val realExists = state.lexemeList.any { it.id == lexemeId }
        val notInDbExists = state.lexemeList.any { it.id == NOT_IN_DB }
        val newList = when {
            realExists -> state.lexemeList.map { l ->
                if (l.id != lexemeId) l
                else if (translation == null) l.copy(translation = null)
                else {
                    val current = l.translation
                    if (current == null) {
                        l.copy(
                            translation = TextValueState(
                                origin = translation,
                                isEdit = false,
                                edited = "",
                            )
                        )
                    } else {
                        l.copy(translation = current.copy(origin = translation))
                    }
                }
            }
            notInDbExists -> state.lexemeList.map { l ->
                if (l.id != NOT_IN_DB) l
                else l.copy(
                    id = lexemeId,
                    translation = translation?.let {
                        TextValueState(origin = it, isEdit = false, edited = "")
                    },
                )
            }
            else -> state.lexemeList
        }
        return state.copy(isPendingDbOp = false, lexemeList = newList) to emptySet()
    }

    private fun refreshDefinition(
        state: WordCardState,
        lexemeId: Long,
        definition: String?,
    ): ReducerResult<WordCardState, Effect> {
        val realExists = state.lexemeList.any { it.id == lexemeId }
        val notInDbExists = state.lexemeList.any { it.id == NOT_IN_DB }
        val newList = when {
            realExists -> state.lexemeList.map { l ->
                if (l.id != lexemeId) l
                else if (definition == null) l.copy(definition = null)
                else {
                    val current = l.definition
                    if (current == null) {
                        l.copy(
                            definition = TextValueState(
                                origin = definition,
                                isEdit = false,
                                edited = "",
                            )
                        )
                    } else {
                        l.copy(definition = current.copy(origin = definition))
                    }
                }
            }
            notInDbExists -> state.lexemeList.map { l ->
                if (l.id != NOT_IN_DB) l
                else l.copy(
                    id = lexemeId,
                    definition = definition?.let {
                        TextValueState(origin = it, isEdit = false, edited = "")
                    },
                )
            }
            else -> state.lexemeList
        }
        return state.copy(isPendingDbOp = false, lexemeList = newList) to emptySet()
    }
}

/** true ⇒ Msg блокируется глобальным guard'ом isPendingDbOp. */
private fun Msg.isGuardedByPending(): Boolean = when (this) {
    is Msg.RemoveWord,
    Msg.CommitWordChanges,
    is Msg.RemoveLexeme,
    is Msg.CommitTranslationEdit,
    is Msg.RemoveTranslation,
    is Msg.CommitDefinitionEdit,
    is Msg.RemoveDefinition,
    Msg.OpenTopBarMenu,
    Msg.OpenDeleteWordDialog,
    is Msg.OpenDeleteLexemeDialog,
    Msg.EnterWordEditMode,
    Msg.CreateLexeme,
    is Msg.CreateTranslation,
    is Msg.EnterTranslationEditMode,
    is Msg.CreateDefinition,
    is Msg.EnterDefinitionEditMode -> true
    else -> false
}
