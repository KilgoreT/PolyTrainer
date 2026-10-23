package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import io.github.kilgoret.mate.then
import io.github.kilgoret.mate.withEffect
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.toRef

/**
 * IS481 generic reducer. ЭТАП 0: скелет — простые (unchanged) ветки реальны,
 * generic-компонентные и flush-on-back — заглушки (этап 4). Структура guard +
 * post-step (§6.1) реальна.
 *
 * IS493 Э5: блок групп — ветки-цепочки атомов [GroupBlockAtoms]
 * (конвенция StateAtoms/ReducerLogging; старые ветки конвенцией не
 * покрыты — мигрируют по мере правок).
 */
class WordCardReducer(
    logger: me.apomazkin.logger.LexemeLogger,
) : CandidatesAtoms(logger),
    MateReducer<WordCardState, Msg, Effect> {
    override fun reduce(
        state: WordCardState,
        message: Msg,
    ): ReducerResult<WordCardState, Effect> {
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
    ): ReducerResult<WordCardState, Effect> =
        when (message) {
            // ===== Top bar =====
            is Msg.OpenTopBarMenu -> state.showMenu() to emptySet()
            is Msg.CloseTopBarMenu ->
                if (!state.topBarState.isMenuOpen) state to emptySet() else state.hideMenu() to emptySet()

            // ===== Delete word =====
            is Msg.OpenDeleteWordDialog ->
                if (state.wordState !is WordState.Loaded) {
                    state to emptySet()
                } else {
                    state.showWordWarningDialog() to emptySet()
                }

            is Msg.CloseDeleteWordDialog -> state.hideWordWarningDialog() to emptySet()

            is Msg.RemoveWord -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null || loaded.id != message.wordId) {
                    state to emptySet()
                } else {
                    state.copy(isPendingDbOp = true).hideWordWarningDialog().hideMenu() to
                        setOf(DatasourceEffect.RemoveWord(wordId = message.wordId))
                }
            }

            // ===== Word edit =====
            is Msg.UpdateWordInput -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null || !loaded.isEditMode) {
                    state to emptySet()
                } else {
                    state.updateWordEdited(message.value) to emptySet()
                }
            }

            is Msg.CommitWordChanges -> {
                val loaded = state.wordState as? WordState.Loaded
                when {
                    loaded == null -> state to emptySet()
                    loaded.edited.isBlank() -> state to emptySet()
                    else ->
                        state.copy(isPendingDbOp = true).disableWordEdit() to
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
                // Решение 2026-07-21: черновик живёт только в открытой карточке —
                // сохранённые пустые лексемы не показываются и тихо удаляются.
                val (empty, alive) = w.lexemeList.partition { it.components.isEmpty() }
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
                    lexemeList = alive.map { it.toLexemeState() },
                ) to
                    buildSet {
                        // Подписки (типы компонентов, группы) включит дифф
                        // subscriptions(): wordState стал Loaded.
                        empty.forEach { add(DatasourceEffect.PurgeEmptyLexeme(w.wordId.id, it.lexemeId.id)) }
                    }
            }

            is Msg.WordNotFound ->
                state.copy(isLoading = false, isPendingDbOp = false) to setOf(NavigationEffect.Back)

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

            is Msg.NoOperation -> state to emptySet()

            // ===== Word edit (commit open edits first) =====
            is Msg.EnterWordEditMode -> {
                if (state.wordState !is WordState.Loaded) {
                    state to emptySet()
                } else {
                    val (committed, effects) = state.commitAndCloseAllEdits()
                    committed.enableWordEdit() to effects
                }
            }

            // ===== Lexeme create =====
            is Msg.CreateLexeme -> {
                if (state.isCreatingLexeme) {
                    state to emptySet()
                } else {
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
                    if (loaded == null) {
                        state to emptySet()
                    } else {
                        state.copy(isPendingDbOp = true, lexemeIdPendingDelete = null) to
                            setOf(DatasourceEffect.RemoveLexeme(loaded.id, message.lexemeId))
                    }
                }
            }

            is Msg.LexemeRemoved -> removeLexemeWithUndo(state, message.removedLexeme)

            is Msg.UndoRestoreLexeme -> {
                val loaded = state.wordState as? WordState.Loaded
                if (loaded == null) {
                    state to emptySet()
                } else {
                    state.copy(isPendingDbOp = true) to
                        setOf(
                            DatasourceEffect.RestoreLexemeWithComponents(
                                loaded.id,
                                loaded.dictionaryId,
                                message.lexeme,
                            ),
                        )
                }
            }

            is Msg.RestoreLexemeFailed ->
                state.copy(isPendingDbOp = false) to
                    setOf(
                        UiEffect.ShowSnackbarWithRetry(
                            messageRes = R.string.word_card_error_restore_lexeme,
                            actionLabelRes = R.string.word_card_action_retry,
                            retryMsg = Msg.UndoRestoreLexeme(message.snapshot),
                        ),
                    )

            // ===== Component value lifecycle =====
            is Msg.CreateComponentValue -> reduceCreateComponentValue(state, message)
            is Msg.UpdateComponentValueInput -> {
                val cv = state
                    .lexemeList
                    .firstOrNull { it.id == message.lexemeId }
                    ?.findByKey(message.key)
                if (cv == null || !cv.isEdit) {
                    state to emptySet()
                } else {
                    state.updateLexeme(message.lexemeId) {
                        it.updateComponent(message.key) { c -> c.copy(edited = message.value) }
                    } to emptySet()
                }
            }

            is Msg.EnterComponentValueEditMode -> {
                val (committed, effects) = state.commitAndCloseAllEdits()
                committed.updateLexeme(message.lexemeId) { lex ->
                    lex.updateComponent(message.key) { c ->
                        c.copy(
                            isEdit = true,
                            edited = c.origin,
                            editedCaption = c.originCaption,
                        )
                    }
                } to effects
            }

            // ===== IS491: captioned_text =====
            is Msg.UpdateComponentCaptionInput -> {
                val cv = state
                    .lexemeList
                    .firstOrNull { it.id == message.lexemeId }
                    ?.findByKey(message.key)
                if (cv == null || !cv.isEdit) {
                    state to emptySet()
                } else {
                    state.updateLexeme(message.lexemeId) {
                        it.updateComponent(message.key) { c -> c.copy(editedCaption = message.caption) }
                    } to emptySet()
                }
            }

            is Msg.LoadCaptionSuggestions ->
                state to setOf(DatasourceEffect.LoadCaptionSuggestions(message.typeId))

            is Msg.CaptionSuggestionsLoaded ->
                state.copy(
                    captionSuggestions = state.captionSuggestions + (message.typeId to message.suggestions),
                ) to emptySet()

            is Msg.CommitComponentValueEdit -> reduceCommitComponentValueEdit(state, message)
            is Msg.RemoveComponentValueRequested -> reduceRemoveComponentValue(state, message)

            // ===== IS486: CHOICE-пикер =====
            is Msg.SelectComponentOption -> reduceSelectComponentOption(state, message)

            // ===== Component types stream =====
            is Msg.ComponentTypesLoaded ->
                state.copy(
                    availableComponentTypes = message.available.types,
                    optionsByType = message.available.optionsByType,
                ) to emptySet()
            is Msg.ComponentTypesLoadFailed ->
                state to
                    setOf(
                        UiEffect.ShowSnackbarWithRetry(
                            messageRes = R.string.word_card_error_load_component_types,
                            actionLabelRes = R.string.word_card_action_retry,
                            retryMsg = Msg.RetryLoadComponentTypes,
                        ),
                    )

            is Msg.RetryLoadComponentTypes -> {
                val loaded = state.wordState as? WordState.Loaded
                // Инкремент typesGeneration ломает equality подписки
                // ComponentTypes — дифф гасит упавшую и стартует новую.
                if (loaded == null) {
                    state to emptySet()
                } else {
                    state.copy(typesGeneration = state.typesGeneration + 1) to emptySet()
                }
            }

            // ===== Datasource re-read =====
            is Msg.RefreshLexemeComponents -> reduceRefreshLexemeComponents(state, message)
            is Msg.ComponentValueInserted -> reduceComponentValueInserted(state, message)
            is Msg.LexemeDraftPromoted -> reduceLexemeDraftPromoted(state, message)

            // ===== Errors / flush-on-back =====
            // ===== IS493 Э5: группы слова (D22) — цепочки атомов =====

            is Msg.OpenGroupPicker -> state.openGroupPicker()

            is Msg.DismissGroupPicker -> state.closeGroupPicker()

            is Msg.ToggleGroupMembership -> {
                val loaded = state.wordState as? WordState.Loaded
                when {
                    loaded == null -> state.noOp("toggleMembership: word not loaded")

                    // Спам по галочке — keyed in-flight (В3/А11).
                    message.groupId in state.groupsBlock.inFlight ->
                        state.noOp("toggleMembership: in flight")

                    // Направление — от факта БД (wordGroupIds, D22.3).
                    message.groupId in state.groupsBlock.wordGroupIds ->
                        state
                            .begin<WordCardState, Effect>()
                            .then { it.markMembershipInFlight(message.groupId) }
                            .withEffect(
                                DatasourceEffect.RemoveMembership(
                                    wordId = loaded.id,
                                    groupId = message.groupId,
                                ),
                            )

                    else ->
                        state
                            .begin<WordCardState, Effect>()
                            .then { it.markMembershipInFlight(message.groupId) }
                            .withEffect(
                                DatasourceEffect.AddMembership(
                                    wordId = loaded.id,
                                    groupId = message.groupId,
                                ),
                            )
                }
            }

            is Msg.DictGroupsLoaded -> state.applyDictGroups(message.groups)

            is Msg.WordGroupsLoaded -> state.applyWordGroups(message.ids)

            is Msg.MembershipDone -> state.clearMembershipInFlight(message.groupId)

            is Msg.MembershipFailed -> state.clearMembershipInFlight(message.groupId)

            // ===== IS533/IS536: лексемы-кандидаты (WCL) — цепочки атомов CandidatesAtoms =====
            is Msg.ServerAccessLoaded -> state.copy(canAddFromDictionary = message.hasAccess) to emptySet()

            Msg.OpenCandidates -> state.openCandidates()

            // Ответы эффектов диалога принимаются, только если они от текущего запуска (WCL-11.4).
            is Msg.CandidatesFound -> state.onCandidatesRun(message, message.requestId) {
                startCandidatesForm(message)
            }

            is Msg.CandidatesNotFound -> state.onCandidatesRun(message, message.requestId) {
                showCandidatesError(R.string.candidates_not_found)
            }

            is Msg.CandidatesLookupFailed -> state.onCandidatesRun(message, message.requestId) {
                showCandidatesError(message.errorRes)
            }

            // Сервер не ответил или недоступен: текст и «Повторить» (WCL-11.9).
            is Msg.CandidatesLookupUnavailable ->
                state.onCandidatesRun(message, message.requestId) {
                    showCandidatesRetryableError(message.errorRes)
                }

            is Msg.CandidatesFormLoaded -> state.onCandidatesRun(message, message.requestId) {
                begin<WordCardState, Effect>()
                    .then { it.applyCandidatesForm(message.form) }
                    .then { it.applyCandidatesRemarks() }
                    .then { it.applyCandidatesComponentMatchLines() }
                    .then { it.refreshCandidatesActiveValues() }
                    .then { it.refreshCandidatesExists() }
                    .then { it.refreshCandidatesLemmaExists() }
                    .then { it.refreshCandidatesCore() }
                    .then { it.refreshCandidatesSummary() }
            }

            is Msg.CandidatesFormFailed -> state.onCandidatesRun(message, message.requestId) {
                showCandidatesError(message.errorRes)
            }

            // Записано: диалог закрыт, снекбар со счётом (WCL-10); список слова уже перечитан
            // handler'ом.
            is Msg.CandidatesAdded -> state.onCandidatesRun(message, message.requestId) {
                val count = UiEffect.ShowCountSnackbar(
                    messageRes = R.plurals.candidates_added,
                    count = message.lexemeCount,
                )
                hideCandidates().withEffect(count)
            }

            // Перенесено (WCL-15.3): слова диалога больше нет — диалог прочь и выход в список,
            // как после удаления слова.
            is Msg.CandidatesTransferred -> state.onCandidatesRun(message, message.requestId) {
                val (hidden, hiding) = hideCandidates()
                val (left, leaving) = reduceImpl(hidden, Msg.NavigateBack)
                left to (hiding + leaving)
            }

            // Не добавилось (WCL-13.2, WCL-15.6): слово или получатель переноса исчезли —
            // текст без повтора.
            is Msg.CandidatesWordGone -> state.onCandidatesRun(message, message.requestId) {
                showCandidatesError(R.string.candidates_word_missing)
            }

            // Не добавилось (WCL-13.3): компонент выключен — его значения сняты, пометки
            // заново, «Повторить».
            is Msg.CandidatesComponentGone -> state.onCandidatesRun(message, message.requestId) {
                begin<WordCardState, Effect>()
                    .then { it.dropCandidatesComponent(message.failure.typeId) }
                    .then { it.refreshCandidatesActiveValues() }
                    .then { it.refreshCandidatesCore() }
                    .then { it.refreshCandidatesSummary() }
                    .then { it.showCandidatesAddFailure(message.failure) }
            }

            // Не добавилось (WCL-13.1): «Не добавилось» и «Повторить».
            is Msg.CandidatesAddFailed -> state.onCandidatesRun(message, message.requestId) {
                showCandidatesAddFailure(message.failure)
            }

            is Msg.Candidates -> reduceCandidateAction(state, message.action)

            is Msg.OperationFailed -> reduceOperationFailed(state, message)
            is Msg.NavigateBack -> {
                if (state.isExiting) {
                    state to emptySet()
                } else {
                    val (next, effects) = state.copy(isExiting = true).commitAndCloseAllEdits()
                    // Решение 2026-07-21: черновик живёт только в открытой карточке —
                    // выход тихо удаляет сохранённые пустые лексемы (best-effort).
                    val wordId = (next.wordState as? WordState.Loaded)?.id
                    val purge = if (wordId == null) {
                        emptySet()
                    } else {
                        next
                            .lexemeList
                            .filter { it.id != NOT_IN_DB && it.components.isEmpty() }
                            .map { DatasourceEffect.PurgeEmptyLexeme(wordId, it.id) }
                            .toSet()
                    }
                    next to (effects + purge)
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
            // IS491: шаблон типа обязателен в pristine — рендер/коммит ветвятся по нему.
            template = type.template,
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
        val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId } ?: return state to emptySet()
        val cv = lex.findByKey(message.key) ?: return state to emptySet()
        return when (val outcome = cv.commitDecision()) {
            CommitOutcome.NoOp ->
                if (!cv.isEdit) {
                    state to emptySet()
                } else {
                    state.updateLexeme(message.lexemeId) {
                        it.updateComponent(message.key) { c ->
                            c.copy(isEdit = false, edited = "", editedCaption = null)
                        }
                    } to emptySet()
                }

            CommitOutcome.LocalRemove ->
                dropComponentMaybeCascade(
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
                } to setOf(DatasourceEffect.RemoveComponentValue(cvId, lex.id, cv.template))
            }

            is CommitOutcome.Update -> {
                val effect = upsertEffect(loaded, lex, cv, outcome.text, outcome.caption)
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
        val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId } ?: return state to emptySet()
        val cv = lex.findByKey(message.key) ?: return state to emptySet()
        return when {
            cv.isPristine ->
                dropComponentMaybeCascade(
                    state,
                    message.lexemeId,
                    message.key,
                ) to emptySet()

            // «Пустой origin = локальный мусор» — только для шаблонов с редактируемым
            // текстом (IS481). У CHOICE origin пуст ВСЕГДА (payload в selectedOptionId),
            // у IMAGE и будущих не-текстовых — тоже: сохранённое значение обязано
            // удаляться через БД (девайс-баг 2026-07-21; IS491 origin-lossy fix).
            cv.origin.isEmpty() && cv.template.hasEditableText ->
                state.updateLexeme(message.lexemeId) { it.removeComponent(message.key) } to emptySet()
            else -> {
                val cvId = cv.componentValueId!!
                state.copy(isPendingDbOp = true).updateLexeme(message.lexemeId) {
                    it.updateComponent(message.key) { c -> c.copy(isCommitting = true) }
                } to setOf(DatasourceEffect.RemoveComponentValue(cvId, message.lexemeId, cv.template))
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
        val existingByCvId = target
            .components
            .filter { it.componentValueId != null }
            .associateBy { it.componentValueId }
        val savedComps = message.components.map { domain ->
            val existing = existingByCvId[domain.id]
            val newOrigin = domain.data.asText().orEmpty()
            // IS486: origin CHOICE — id опции. IS491: originCaption captioned-значения.
            val newOptionId = (domain.data as? me.apomazkin.lexeme.ChoiceValues)?.optionId
            val newCaption = domain.data.asCaption()
            when {
                existing == null -> domain.toComponentValueState()
                existing.isCommitting ->
                    existing.copy(
                        origin = newOrigin,
                        selectedOptionId = newOptionId,
                        originCaption = newCaption,
                        isEdit = false,
                        isCommitting = false,
                        edited = "",
                        editedCaption = null,
                    )

                existing.isEdit ->
                    existing.copy(
                        origin = newOrigin,
                        selectedOptionId = newOptionId,
                        originCaption = newCaption,
                    )
                else ->
                    existing.copy(
                        origin = newOrigin,
                        selectedOptionId = newOptionId,
                        originCaption = newCaption,
                        isEdit = false,
                    )
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
        val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId } ?: return state to emptySet()
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
            it.isPristine &&
                it.pristineKey != message.anchorPristineKey &&
                it
                    .edited
                    .trim()
                    .isNotEmpty()
        }
        val promoted = message.newLexeme.toLexemeState()
        val survivorStates = survivors.map { it.copy(isCommitting = true) }
        val newLexeme = promoted.copy(components = promoted.components + survivorStates)
        val effects = survivors
            .map { s ->
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = loaded.id,
                    dictionaryId = loaded.dictionaryId,
                    lexemeId = promoted.id,
                    pristineKey = s.pristineKey!!,
                    componentTypeId = s.componentTypeId,
                    componentTypeRef = s.componentTypeRef,
                    data = templateValuesOf(s.template, s.edited.trim(), s.editedCaption),
                )
            }.toSet()
        val newList = state.lexemeList.map { if (it.id == NOT_IN_DB) newLexeme else it }
        return state.copy(isPendingDbOp = false, lexemeList = newList) to effects
    }

    /**
     * IS486: выбор опции CHOICE — прямой коммит без edit-режима, ТОЛЬКО добавление.
     * Смена опции как операция упразднена (решение 2026-07-21: тело чипа глухое,
     * смена = удалить + добавить заново) — пикер открывается только с чипа
     * добавления, который скрыт при существующем значении. Существующее значение
     * типа (guard) и in-flight pristine → игнор. Драфт (NOT_IN_DB) — недостижим
     * правилом участия (CHOICE не ядро), guard на всякий случай.
     */
    private fun reduceSelectComponentOption(
        state: WordCardState,
        message: Msg.SelectComponentOption,
    ): ReducerResult<WordCardState, Effect> {
        val loaded = state.wordState as? WordState.Loaded ?: return state to emptySet()
        val lex = state.lexemeList.firstOrNull { it.id == message.lexemeId }
            ?: return state to emptySet()
        if (lex.id == NOT_IN_DB) return state to emptySet()
        val type = state.availableComponentTypes.firstOrNull { it.id == message.typeId }
            ?: return state to emptySet()
        // Значение типа уже есть (сохранённое или in-flight pristine) — игнор:
        // single-CHOICE не плодится, смена только через удалить+добавить.
        if (lex.components.any { it.componentTypeId == message.typeId }) {
            return state to emptySet()
        }
        val pristine = ComponentValueState(
            key = ComponentValueKey.Pristine(state.nextPristineKey),
            componentTypeId = type.id,
            componentTypeRef = type.toRef(),
            isMultiple = type.isMultiple,
            template = type.template,
            isCommitting = true,
            selectedOptionId = message.optionId,
        )
        return state
            .copy(isPendingDbOp = true, nextPristineKey = state.nextPristineKey + 1)
            .updateLexeme(lex.id) { it.appendPristine(pristine) } to
            setOf(
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = loaded.id,
                    dictionaryId = loaded.dictionaryId,
                    lexemeId = lex.id,
                    pristineKey = pristine.pristineKey!!,
                    componentTypeId = type.id,
                    componentTypeRef = type.toRef(),
                    data = ChoiceValues(message.optionId),
                ),
            )
    }

    private fun reduceOperationFailed(
        state: WordCardState,
        message: Msg.OperationFailed,
    ): ReducerResult<WordCardState, Effect> {
        val cleared = state.copy(
            isPendingDbOp = false,
            isExiting = false,
            lexemeList = state.lexemeList.map { lex ->
                lex.copy(
                    components = lex
                        .components
                        // Осиротевший CHOICE-pristine после провала AddValue — мусорный
                        // чип без пользовательского ввода: удалить (девайс-баг 2026-07-21).
                        .filterNot { it.isPristine && it.isCommitting && it.template == ComponentTemplate.CHOICE }
                        .map { if (it.isCommitting) it.copy(isCommitting = false) else it },
                )
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
        caption: String? = null,
    ): DatasourceEffect.UpsertComponentValue =
        when {
            lex.id == NOT_IN_DB ->
                DatasourceEffect.UpsertComponentValue.CreateLexeme(
                    wordId = loaded.id,
                    dictionaryId = loaded.dictionaryId,
                    pristineKey = cv.pristineKey!!,
                    componentTypeId = cv.componentTypeId,
                    componentTypeRef = cv.componentTypeRef,
                    data = templateValuesOf(cv.template, text, caption),
                )

            cv.componentValueId != null ->
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = loaded.id,
                    dictionaryId = loaded.dictionaryId,
                    lexemeId = lex.id,
                    componentValueId = cv.componentValueId!!,
                    componentTypeId = cv.componentTypeId,
                    componentTypeRef = cv.componentTypeRef,
                    data = templateValuesOf(cv.template, text, caption),
                )

            else ->
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = loaded.id,
                    dictionaryId = loaded.dictionaryId,
                    lexemeId = lex.id,
                    pristineKey = cv.pristineKey!!,
                    componentTypeId = cv.componentTypeId,
                    componentTypeRef = cv.componentTypeRef,
                    data = templateValuesOf(cv.template, text, caption),
                )
        }

    // ===== IS533/IS536: диалог лексем-кандидатов (WCL) =====

    /** Диалог открыт и это ответ его запуска (WCL-11.4). */
    private fun WordCardState.isCurrentCandidatesRun(requestId: Long): Boolean =
        candidates?.requestId == requestId

    /**
     * Ветка ответа эффекта диалога: ответ чужого запуска отбрасывается no-op'ом
     * с именем сообщения, ответ текущего — [reduce] (WCL-11.4).
     */
    private fun WordCardState.onCandidatesRun(
        message: Msg,
        requestId: Long,
        reduce: WordCardState.() -> ReducerResult<WordCardState, Effect>,
    ): ReducerResult<WordCardState, Effect> =
        if (isCurrentCandidatesRun(requestId)) {
            reduce()
        } else {
            noOp("${message::class.simpleName}: foreign run $requestId")
        }

    /**
     * Действия общих виджетов диалога (WCL-09, WCL-10, WCL-12): цепочки
     * атомов [CandidatesAtoms]; страховок от повторного нажатия нет —
     * кнопки и листы неактивны по флагам стейта; шагов окна снимка, которых
     * в диалоге нет (группы, «куда класть», вопрос перед записью), — no-op.
     */
    private fun reduceCandidateAction(
        state: WordCardState,
        action: CandidateAction,
    ): ReducerResult<WordCardState, Effect> {
        val dialog = state.candidates ?: return state.noOp("candidates: dialog closed")
        return when (action) {
            is CandidateAction.DeleteCard -> state
                .begin<WordCardState, Effect>()
                .then { it.deleteCandidateCard(action.lemma) }
                .then { it.markCandidatesDirty() }
                .then { it.refreshCandidatesLemmaExists() }
                .then { it.refreshCandidatesSummary() }

            is CandidateAction.DeleteLexeme -> state
                .begin<WordCardState, Effect>()
                .then { it.deleteCandidateLexeme(action.lemma, action.key) }
                .then { it.markCandidatesDirty() }
                .then { it.refreshCandidatesLemmaExists() }
                .then { it.refreshCandidatesSummary() }

            // Значения компонента: зависимые, «есть» и ядро пересчитываются, как после правки текста.
            is CandidateAction.DeleteValue -> state
                .begin<WordCardState, Effect>()
                .then { it.deleteCandidateValue(action.ref) }
                .then { it.refreshCandidatesActiveValues() }
                .then { it.markCandidatesDirty() }
                .then { it.refreshCandidatesExists() }
                .then { it.refreshCandidatesLemmaExists() }
                .then { it.refreshCandidatesCore() }
                .then { it.refreshCandidatesSummary() }

            is CandidateAction.OpenSheet -> state.openCandidatesSheet(action.sheet)

            CandidateAction.CloseSheet -> state.closeCandidatesSheet()

            is CandidateAction.UpdateDraft -> state.updateCandidatesDraft(action.text)

            is CandidateAction.SubmitText -> reduceCandidatesSubmitText(state, dialog, action)

            is CandidateAction.ResetText -> reduceCandidatesResetText(state, dialog, action)

            is CandidateAction.SetOption -> reduceCandidatesSetOption(state, dialog, action)

            CandidateAction.ToggleComponentMatches -> state.toggleCandidatesComponentMatches()

            // Выбор меняет, что считается «есть» в сводке при переносе (WCL-15.4).
            is CandidateAction.ToggleLemmaWord -> state
                .begin<WordCardState, Effect>()
                .then { it.toggleCandidatesLemmaWord(action.choice) }
                .then { it.refreshCandidatesSummary() }

            // Ш4: без вопроса — диалог сам подтверждение (WCL-10).
            CandidateAction.RequestAdd -> state.startAddingCandidates()

            CandidateAction.RetryAdd -> state
                .begin<WordCardState, Effect>()
                .then { it.clearCandidatesAddFailure() }
                .then { it.startAddingCandidates() }

            // «Повторить» запрос на ошибке сервера: новый запуск (WCL-11.9).
            CandidateAction.RetryLookup -> state.retryCandidatesLookup()

            // В2: закрытие — сразу, с вопросом после правок (WCL-12).
            CandidateAction.RequestClose -> when {
                dialog.isDirty -> state.askCandidatesClose()
                else -> state.hideCandidates()
            }

            CandidateAction.ConfirmClose -> state.hideCandidates()

            CandidateAction.Stay -> state.closeCandidatesClose()

            CandidateAction.ConfirmAdd,
            CandidateAction.CancelAdd,
            CandidateAction.ToggleNotAdded,
            is CandidateAction.SetCardTarget,
            is CandidateAction.SetCardGroup,
            is CandidateAction.SetCommonGroup,
            -> state.noOp("candidates: no such step in the dialog")
        }
    }

    /**
     * «Сохранить» в листе правки: обрезать, стёртое прочь, зависимые значения
     * заново, лист закрыть, флаг правок — только если текст и правда сменился
     * (WCL-12.5), пометки и сводка. Условие по стейту, тест редьюсера.
     */
    private fun reduceCandidatesSubmitText(
        state: WordCardState,
        dialog: CandidatesState,
        action: CandidateAction.SubmitText,
    ): ReducerResult<WordCardState, Effect> {
        val changed = dialog.textChanged(action.ref, action.text)
        return state
            .begin<WordCardState, Effect>()
            .then { it.applyCandidateTextEdit(action.ref, action.text) }
            .then { it.dropCandidateEmptyTexts(action.ref) }
            .then { it.refreshCandidatesActiveValues() }
            .then { it.closeCandidatesSheet() }
            .then { if (changed) it.markCandidatesDirty() else it.begin() }
            .then { it.refreshCandidatesExists() }
            .then { it.refreshCandidatesLemmaExists() }
            .then { it.refreshCandidatesCore() }
            .then { it.refreshCandidatesSummary() }
    }

    /**
     * «Сбросить» в листе правки: флаг правок — только если текст отличался от
     * текста ответа (WCL-12.5). Зависимые значения не пересчитываются: текст
     * ответа не пуст, а стёртого значения уже нет (AWP-15.6).
     */
    private fun reduceCandidatesResetText(
        state: WordCardState,
        dialog: CandidatesState,
        action: CandidateAction.ResetText,
    ): ReducerResult<WordCardState, Effect> {
        val changed = dialog.isTextEdited(action.ref)
        return state
            .begin<WordCardState, Effect>()
            .then { it.resetCandidateText(action.ref) }
            .then { it.closeCandidatesSheet() }
            .then { if (changed) it.markCandidatesDirty() else it.begin() }
            .then { it.refreshCandidatesExists() }
            .then { it.refreshCandidatesLemmaExists() }
            .then { it.refreshCandidatesCore() }
            .then { it.refreshCandidatesSummary() }
    }

    /**
     * Опция «Выбора»: зависимости заново, лист закрыть, флаг правок — только
     * если опция сменилась (WCL-12.5). «Есть» и ядро не трогает: «Выбор» — не ядро.
     */
    private fun reduceCandidatesSetOption(
        state: WordCardState,
        dialog: CandidatesState,
        action: CandidateAction.SetOption,
    ): ReducerResult<WordCardState, Effect> {
        val changed = dialog.optionChanged(action.ref, action.option)
        return state
            .begin<WordCardState, Effect>()
            .then { it.setCandidateOption(action.ref, action.option) }
            .then { it.refreshCandidatesActiveValues() }
            .then { it.closeCandidatesSheet() }
            .then { if (changed) it.markCandidatesDirty() else it.begin() }
    }
}

/** true ⇒ Msg блокируется guard'ом isPendingDbOp / isExiting.
 * Э5 (ревью Mate-5): интенты групп гейтятся (пикер не открывается над
 * умирающей карточкой); Done/Failed/Loaded — НЕ гейтятся (иначе
 * потеряется снятие in-flight). */
private fun Msg.isGuardedByPending(): Boolean =
    when (this) {
        is Msg.RemoveWord,
        Msg.CommitWordChanges,
        is Msg.RemoveLexeme,
        is Msg.CommitComponentValueEdit,
        is Msg.RemoveComponentValueRequested,
        is Msg.EnterComponentValueEditMode,
        is Msg.SelectComponentOption,
        Msg.OpenTopBarMenu,
        Msg.OpenDeleteWordDialog,
        is Msg.OpenDeleteLexemeDialog,
        Msg.EnterWordEditMode,
        Msg.CreateLexeme,
        Msg.OpenGroupPicker,
        is Msg.ToggleGroupMembership,
        -> true

        else -> false
    }
