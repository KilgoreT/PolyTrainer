package me.apomazkin.wordcard.mate

import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.toRef
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.ReducerResult

/**
 * IS481 generic reducer. ЭТАП 0: скелет — простые (unchanged) ветки реальны,
 * generic-компонентные и flush-on-back — заглушки (этап 4). Структура guard +
 * post-step (§6.1) реальна.
 */
class WordCardReducer : MateReducer<WordCardState, Msg, Effect> {

    override fun reduce(state: WordCardState, message: Msg): ReducerResult<WordCardState, Effect> {
        if ((state.isPendingDbOp || state.isExiting) && message.isGuardedByPending()) {
            return state to emptySet()
        }
        val (next, effects) = reduceImpl(state, message)
        // Flush-on-back (§6.2.3): Back на ПЕРЕХОДЕ в (isExiting && !hasInFlightCommits), isExiting НЕ сбрасываем.
        val readyNow = next.isExiting && !next.hasInFlightCommits
        val readyBefore = state.isExiting && !state.hasInFlightCommits
        return if (readyNow && !readyBefore) {
            next to (effects + NavigationEffect.Back)
        } else {
            next to effects
        }
    }

    private fun reduceImpl(
        state: WordCardState,
        message: Msg,
    ): ReducerResult<WordCardState, Effect> {
        return when (message) {
            // ===== Top bar =====
            is Msg.OpenTopBarMenu -> state.showMenu() to emptySet()
            is Msg.CloseTopBarMenu ->
                if (!state.topBarState.isMenuOpen) state to emptySet() else state.hideMenu() to emptySet()

            // ===== Delete word =====
            is Msg.OpenDeleteWordDialog ->
                if (state.wordState !is WordState.Loaded) state to emptySet()
                else state.showWordWarningDialog() to emptySet()

            is Msg.CloseDeleteWordDialog -> state.hideWordWarningDialog() to emptySet()

            is Msg.RemoveWord -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null || loaded.id != message.wordId) state to emptySet()
                else state.copy(isPendingDbOp = true).hideWordWarningDialog().hideMenu() to
                        setOf(DatasourceEffect.RemoveWord(wordId = message.wordId))
            }

            // ===== Word edit =====
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
                    else -> state.copy(isPendingDbOp = true).disableWordEdit() to
                            setOf(
                                DatasourceEffect.UpdateWord(
                                    wordId = loaded.id,
                                    value = loaded.edited,
                                ),
                            )
                }
            }

            // ===== Lexeme delete dialog =====
            is Msg.OpenDeleteLexemeDialog ->
                state.copy(lexemeIdPendingDelete = message.lexemeId) to emptySet()

            is Msg.CloseDeleteLexemeDialog ->
                state.copy(lexemeIdPendingDelete = null) to emptySet()

            // ===== Datasource load =====
            is Msg.WordLoaded -> {
                val w = message.word
                state.copy(
                    isLoading = false,
                    isPendingDbOp = false,
                    wordState = WordState.Loaded(
                        id = w.wordId.id,
                        dictionaryId = w.dictionaryId,
                        dictionaryFlagRes = w.dictionaryFlagRes,
                        added = w.addedDate,
                        value = w.word.value,
                    ),
                    lexemeList = w.lexemeList.map { it.toLexemeState() },
                ) to setOf(DatasourceEffect.LoadAvailableComponentTypes(w.dictionaryId))
            }

            is Msg.WordNotFound ->
                state.copy(isLoading = false, isPendingDbOp = false) to setOf(NavigationEffect.Back)

            is Msg.RefreshWord -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null) state.copy(isPendingDbOp = false) to emptySet()
                else state.copy(
                    isPendingDbOp = false,
                    wordState = loaded.copy(
                        value = message.word.word.value,
                        isEditMode = false,
                        edited = "",
                    ),
                ) to emptySet()
            }

            is Msg.NoOperation -> state to emptySet()

            // ===== Word edit (commit open edits first) =====
            is Msg.EnterWordEditMode -> {
                if (state.wordState !is WordState.Loaded) state to emptySet()
                else {
                    val (committed, effects) = state.commitAndCloseAllEdits()
                    committed.enableWordEdit() to effects
                }
            }

            // ===== Lexeme create =====
            is Msg.CreateLexeme -> {
                if (state.isCreatingLexeme) state to emptySet()
                else {
                    val (committed, effects) = state.commitAndCloseAllEdits()
                    committed.copy(lexemeList = listOf(LexemeState(id = NOT_IN_DB)) + committed.lexemeList) to effects
                }
            }

            // ===== Lexeme remove / undo =====
            is Msg.RemoveLexeme -> {
                if (message.lexemeId == NOT_IN_DB) {
                    state.removeLexeme(NOT_IN_DB).copy(lexemeIdPendingDelete = null) to emptySet()
                } else {
                    val loaded = state.wordState as? WordState.Loaded
                    if (loaded == null) state to emptySet()
                    else state.copy(isPendingDbOp = true, lexemeIdPendingDelete = null) to
                            setOf(DatasourceEffect.RemoveLexeme(loaded.id, message.lexemeId))
                }
            }

            is Msg.LexemeCascadeRemoved -> removeLexemeWithUndo(state, message.removedLexeme)
            is Msg.LexemeRemoved -> removeLexemeWithUndo(state, message.removedLexeme)

            is Msg.UndoRestoreLexeme -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null) state to emptySet()
                else state.copy(isPendingDbOp = true) to
                        setOf(
                            DatasourceEffect.RestoreLexemeWithComponents(
                                loaded.id,
                                loaded.dictionaryId,
                                message.lexeme,
                            ),
                        )
            }

            is Msg.RestoreLexemeFailed ->
                state.copy(isPendingDbOp = false) to setOf(
                    UiEffect.ShowSnackbarWithRetry(
                        messageRes = R.string.word_card_error_restore_lexeme,
                        actionLabelRes = R.string.word_card_action_retry,
                        retryMsg = Msg.UndoRestoreLexeme(message.snapshot),
                    ),
                )

            // ===== Component value lifecycle =====
            is Msg.CreateComponentValue -> reduceCreateComponentValue(state, message)
            is Msg.UpdateComponentValueInput -> {
                val cv = state.lexemeList.firstOrNull { it.id == message.lexemeId }
                    ?.findByKey(message.key)
                if (cv == null || !cv.isEdit) state to emptySet()
                else state.updateLexeme(message.lexemeId) {
                    it.updateComponent(message.key) { c -> c.copy(edited = message.value) }
                } to emptySet()
            }

            is Msg.EnterComponentValueEditMode -> {
                val (committed, effects) = state.commitAndCloseAllEdits()
                committed.updateLexeme(message.lexemeId) { lex ->
                    lex.updateComponent(message.key) { c ->
                        c.copy(
                            isEdit = true,
                            edited = c.origin,
                        )
                    }
                } to effects
            }

            is Msg.CommitComponentValueEdit -> reduceCommitComponentValueEdit(state, message)
            is Msg.RemoveComponentValueRequested -> reduceRemoveComponentValue(state, message)

            // ===== Component types stream =====
            is Msg.ComponentTypesLoaded -> state.copy(availableComponentTypes = message.types) to emptySet()
            is Msg.ComponentTypesLoadFailed -> state to setOf(
                UiEffect.ShowSnackbarWithRetry(
                    messageRes = R.string.word_card_error_load_component_types,
                    actionLabelRes = R.string.word_card_action_retry,
                    retryMsg = Msg.RetryLoadComponentTypes,
                ),
            )

            is Msg.RetryLoadComponentTypes -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null) state to emptySet()
                else state to setOf(DatasourceEffect.LoadAvailableComponentTypes(loaded.dictionaryId))
            }

            // ===== Datasource re-read =====
            is Msg.RefreshLexemeComponents -> reduceRefreshLexemeComponents(state, message)
            is Msg.ComponentValueInserted -> reduceComponentValueInserted(state, message)
            is Msg.LexemeDraftPromoted -> reduceLexemeDraftPromoted(state, message)

            // ===== Errors / flush-on-back =====
            is Msg.OperationFailed -> reduceOperationFailed(state, message)
            is Msg.NavigateBack -> {
                if (state.isExiting) state to emptySet()
                else state.copy(isExiting = true).commitAndCloseAllEdits()
            }
        }
    }

    private fun removeLexemeWithUndo(
        state: WordCardState,
        removed: me.apomazkin.lexeme.Lexeme,
    ): ReducerResult<WordCardState, Effect> {
        val id = removed.lexemeId.id
        val next = state.copy(
            isPendingDbOp = false,
            lexemeList = state.lexemeList.filterNot { it.id == id },
        )
        // При flush-on-back экран тут же закрывается (пост-шаг Back) — undo-снек бесполезен.
        val effects: Set<Effect> = if (next.isExiting) {
            emptySet()
        } else {
            setOf(
                UiEffect.ShowSnackbarWithUndo(
                    messageRes = R.string.word_card_snackbar_lexeme_deleted,
                    actionLabelRes = R.string.word_card_snackbar_undo,
                    undoMsg = Msg.UndoRestoreLexeme(removed),
                ),
            )
        }
        return next to effects
    }

    private fun reduceCreateComponentValue(
        state: WordCardState,
        message: Msg.CreateComponentValue,
    ): ReducerResult<WordCardState, Effect> {
        val type = state.availableComponentTypes.firstOrNull { it.id == message.typeId }
            ?: return state to emptySet()
        val (committed, effects) = state.commitAndCloseAllEdits()
        val pristine = ComponentValueState(
            key = ComponentValueKey.Pristine(committed.nextPristineKey),
            componentTypeId = type.id,
            componentTypeRef = type.toRef(),
            isMultiple = type.isMultiple,
            isEdit = true,
        )
        return when {
            committed.lexemeList.any { it.id == message.lexemeId } ->
                committed
                    .updateLexeme(message.lexemeId) { it.appendPristine(pristine) }
                    .copy(nextPristineKey = committed.nextPristineKey + 1) to effects

            // target — пустой NOT_IN_DB черновик, выкинутый commitAndCloseAllEdits: восстановить с pristine.
            message.lexemeId == NOT_IN_DB ->
                committed.copy(
                    lexemeList = listOf(
                        LexemeState(
                            id = NOT_IN_DB,
                            components = listOf(pristine),
                        ),
                    ) +
                            committed.lexemeList,
                    nextPristineKey = committed.nextPristineKey + 1,
                ) to effects

            // target — real лексема, исчезнувшая до коммита (гонка с удалением): не фабриковать фантом.
            else -> committed to effects
        }
    }

    private fun reduceCommitComponentValueEdit(
        state: WordCardState,
        message: Msg.CommitComponentValueEdit,
    ): ReducerResult<WordCardState, Effect> {
        val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
        val lex =
            state.lexemeList.firstOrNull { it.id == message.lexemeId } ?: return state to emptySet()
        val cv = lex.findByKey(message.key) ?: return state to emptySet()
        return when (val outcome = cv.commitDecision()) {
            CommitOutcome.NoOp ->
                if (!cv.isEdit) state to emptySet()
                else state.updateLexeme(message.lexemeId) {
                    it.updateComponent(message.key) { c -> c.copy(isEdit = false, edited = "") }
                } to emptySet()

            CommitOutcome.LocalRemove -> dropComponentMaybeCascade(
                state,
                message.lexemeId,
                message.key,
            ) to emptySet()

            CommitOutcome.PessimisticRemove -> {
                val cvId = cv.componentValueId ?: return dropComponentMaybeCascade(
                    state,
                    message.lexemeId,
                    message.key,
                ) to emptySet()
                state.copy(isPendingDbOp = true).updateLexeme(message.lexemeId) {
                    it.updateComponent(message.key) { c -> c.copy(isCommitting = true) }
                } to setOf(DatasourceEffect.RemoveComponentValue(cvId, lex.id))
            }

            is CommitOutcome.Update -> {
                val effect = upsertEffect(loaded, lex, cv, outcome.text)
                state.copy(isPendingDbOp = true).updateLexeme(message.lexemeId) {
                    it.updateComponent(message.key) { c -> c.copy(isCommitting = true) }
                } to setOf(effect)
            }
        }
    }

    private fun reduceRemoveComponentValue(
        state: WordCardState,
        message: Msg.RemoveComponentValueRequested,
    ): ReducerResult<WordCardState, Effect> {
        val lex =
            state.lexemeList.firstOrNull { it.id == message.lexemeId } ?: return state to emptySet()
        val cv = lex.findByKey(message.key) ?: return state to emptySet()
        return when {
            cv.isPristine -> dropComponentMaybeCascade(
                state,
                message.lexemeId,
                message.key,
            ) to emptySet()

            cv.origin.isEmpty() -> state.updateLexeme(message.lexemeId) { it.removeComponent(message.key) } to emptySet()
            else -> {
                val cvId = cv.componentValueId!!
                state.copy(isPendingDbOp = true).updateLexeme(message.lexemeId) {
                    it.updateComponent(message.key) { c -> c.copy(isCommitting = true) }
                } to setOf(DatasourceEffect.RemoveComponentValue(cvId, message.lexemeId))
            }
        }
    }

    private fun reduceRefreshLexemeComponents(
        state: WordCardState,
        message: Msg.RefreshLexemeComponents,
    ): ReducerResult<WordCardState, Effect> {
        val cleared = state.copy(isPendingDbOp = false)
        val target = cleared.lexemeList.firstOrNull { it.id == message.lexemeId }
            ?: return cleared to emptySet()
        val existingByCvId = target.components
            .filter { it.componentValueId != null }
            .associateBy { it.componentValueId }
        val savedComps = message.components.map { domain ->
            val existing = existingByCvId[domain.id]
            val newOrigin = domain.data.asText().orEmpty()
            when {
                existing == null -> domain.toComponentValueState()
                existing.isCommitting -> existing.copy(
                    origin = newOrigin,
                    isEdit = false,
                    isCommitting = false,
                    edited = "",
                )

                existing.isEdit -> existing.copy(origin = newOrigin)
                else -> existing.copy(origin = newOrigin, isEdit = false)
            }
        }
        val pristineTail = target.components.filter { it.isPristine }
        val merged = target.copy(components = savedComps + pristineTail)
        return cleared.updateLexeme(message.lexemeId) { merged } to emptySet()
    }

    private fun reduceComponentValueInserted(
        state: WordCardState,
        message: Msg.ComponentValueInserted,
    ): ReducerResult<WordCardState, Effect> {
        val lex =
            state.lexemeList.firstOrNull { it.id == message.lexemeId } ?: return state to emptySet()
        val pristine = lex.components.firstOrNull { it.pristineKey == message.pristineKey }
            ?: return state to emptySet()
        val savedKey = ComponentValueKey.Saved(message.newCvId)
        val updated = if (lex.components.any { it.key == savedKey }) {
            lex.removeComponent(pristine.key)
        } else {
            lex.updateComponent(pristine.key) { c ->
                c.copy(
                    key = savedKey,
                    isEdit = false,
                    isCommitting = false,
                )
            }
        }
        return state.updateLexeme(message.lexemeId) { updated } to emptySet()
    }

    private fun reduceLexemeDraftPromoted(
        state: WordCardState,
        message: Msg.LexemeDraftPromoted,
    ): ReducerResult<WordCardState, Effect> {
        val loaded = state.wordState as? WordState.Loaded
            ?: return state.copy(isPendingDbOp = false) to emptySet()
        val draft = state.lexemeList.firstOrNull { it.id == NOT_IN_DB }
            ?: return state.copy(isPendingDbOp = false) to emptySet()
        val survivors = draft.components.filter {
            it.isPristine && it.pristineKey != message.anchorPristineKey && it.edited.trim()
                .isNotEmpty()
        }
        val promoted = message.newLexeme.toLexemeState()
        val survivorStates = survivors.map { it.copy(isCommitting = true) }
        val newLexeme = promoted.copy(components = promoted.components + survivorStates)
        val effects = survivors.map { s ->
            DatasourceEffect.UpsertComponentValue.AddValue(
                wordId = loaded.id,
                dictionaryId = loaded.dictionaryId,
                lexemeId = promoted.id,
                pristineKey = s.pristineKey!!,
                componentTypeId = s.componentTypeId,
                componentTypeRef = s.componentTypeRef,
                data = textValuesOf(s.edited.trim()),
            )
        }.toSet()
        val newList = state.lexemeList.map { if (it.id == NOT_IN_DB) newLexeme else it }
        return state.copy(isPendingDbOp = false, lexemeList = newList) to effects
    }

    private fun reduceOperationFailed(
        state: WordCardState,
        message: Msg.OperationFailed,
    ): ReducerResult<WordCardState, Effect> {
        val cleared = state.copy(
            isPendingDbOp = false,
            isExiting = false,
            lexemeList = state.lexemeList.map { lex ->
                lex.copy(components = lex.components.map { if (it.isCommitting) it.copy(isCommitting = false) else it })
            },
        )
        return cleared to setOf(UiEffect.ShowErrorSnackbar(message.messageRes))
    }

    /** Удалить компонент локально; если NOT_IN_DB лексема осталась без компонентов — удалить её (cascade). */
    private fun dropComponentMaybeCascade(
        state: WordCardState,
        lexemeId: Long,
        key: ComponentValueKey,
    ): WordCardState {
        val afterRemove = state.updateLexeme(lexemeId) { it.removeComponent(key) }
        return afterRemove.copy(
            lexemeList = afterRemove.lexemeList.filterNot { it.id == NOT_IN_DB && it.components.isEmpty() },
        )
    }

    /** Эффект upsert по контексту: NOT_IN_DB→CreateLexeme, saved→UpdateValue, real-pristine→AddValue. */
    private fun upsertEffect(
        loaded: WordState.Loaded,
        lex: LexemeState,
        cv: ComponentValueState,
        text: String,
    ): DatasourceEffect.UpsertComponentValue = when {
        lex.id == NOT_IN_DB -> DatasourceEffect.UpsertComponentValue.CreateLexeme(
            wordId = loaded.id,
            dictionaryId = loaded.dictionaryId,
            pristineKey = cv.pristineKey!!,
            componentTypeId = cv.componentTypeId,
            componentTypeRef = cv.componentTypeRef,
            data = textValuesOf(text),
        )

        cv.componentValueId != null -> DatasourceEffect.UpsertComponentValue.UpdateValue(
            wordId = loaded.id,
            dictionaryId = loaded.dictionaryId,
            lexemeId = lex.id,
            componentValueId = cv.componentValueId!!,
            componentTypeId = cv.componentTypeId,
            componentTypeRef = cv.componentTypeRef,
            data = textValuesOf(text),
        )

        else -> DatasourceEffect.UpsertComponentValue.AddValue(
            wordId = loaded.id,
            dictionaryId = loaded.dictionaryId,
            lexemeId = lex.id,
            pristineKey = cv.pristineKey!!,
            componentTypeId = cv.componentTypeId,
            componentTypeRef = cv.componentTypeRef,
            data = textValuesOf(text),
        )
    }
}

/** true ⇒ Msg блокируется guard'ом isPendingDbOp / isExiting. */
private fun Msg.isGuardedByPending(): Boolean = when (this) {
    is Msg.RemoveWord,
    Msg.CommitWordChanges,
    is Msg.RemoveLexeme,
    is Msg.CommitComponentValueEdit,
    is Msg.RemoveComponentValueRequested,
    is Msg.EnterComponentValueEditMode,
    Msg.OpenTopBarMenu,
    Msg.OpenDeleteWordDialog,
    is Msg.OpenDeleteLexemeDialog,
    Msg.EnterWordEditMode,
    Msg.CreateLexeme,
        -> true

    else -> false
}
