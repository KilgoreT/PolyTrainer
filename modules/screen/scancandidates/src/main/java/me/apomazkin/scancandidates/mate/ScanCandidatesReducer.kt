package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.begin
import io.github.kilgoret.mate.then
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.logger.LexemeLogger

/**
 * Редьюсер экрана лексем-кандидатов: сообщение — видимая цепочка атомов
 * [ScanCandidatesAtoms], сводка кнопки последней. Ветвится только по стейту,
 * исходы юзкейсов не разбирает: хендлер прислал сообщение на каждый случай.
 * Условий два: «текст сменился» и «опция сменилась» перед флагом правок и
 * `isDirty` при закрытии — инварианты спеки (AWP-19). Страховок от повторного
 * нажатия нет: кнопки, листы и крестик неактивны по флагам стейта.
 */
class ScanCandidatesReducer(
    logger: LexemeLogger,
) : ScanCandidatesAtoms(logger),
    MateReducer<ScanCandidatesState, Msg, Effect> {
    override fun reduce(
        state: ScanCandidatesState,
        message: Msg,
    ): ScanCandidatesResult {
        logMessage(message.logName())
        return reduceMessage(state, message)
    }

    /** Тяжёлые сообщения — в лог именем класса, содержимое компактно пишет хендлер. */
    private fun Msg.logName(): String = when (this) {
        is Msg.TextRecognized,
        is Msg.WordsFound,
        is Msg.FormLoaded,
        -> this::class.simpleName.orEmpty()
        else -> toString()
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun reduceMessage(
        state: ScanCandidatesState,
        message: Msg,
    ): ScanCandidatesResult = when (message) {
        // ===== От эффектов: снимок, запрос, форма =====

        is Msg.TextRecognized -> state.startLooking(message.tokens)

        Msg.TextEmpty -> state.showNoWords()

        is Msg.RecognitionFailed -> state.showError(message.errorRes)

        is Msg.WordsFound -> state.startFormLoading(message)

        Msg.WordsEmpty -> state.showNoWords()

        is Msg.LookupDenied -> state.showError(message.errorRes)

        is Msg.LookupUnavailable -> state.showRetryableError(message.errorRes)

        is Msg.FormLoaded -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.applyForm(message.form) }
            .then { it.applyRemarks() }
            .then { it.applyComponentMatchLines() }
            .then { it.refreshActiveValues() }
            .then { it.refreshExistsMarks() }
            .then { it.refreshCoreMarks() }
            .then { it.refreshSummary() }

        is Msg.FormFailed -> state.showError(message.errorRes)

        // ===== От эффектов: запись (В3, AWP-21) =====

        is Msg.AddFinished -> state.closeWithResult(message.lexemeCount, message.wordCount)

        is Msg.AddGroupGone -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.resetDeadGroup(message.failure.groupId) }
            .then { it.showAddFailure(message.failure) }

        is Msg.AddWordGone -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.resetDeadTarget(message.failure.wordId) }
            .then { it.showAddFailure(message.failure) }

        is Msg.AddComponentGone -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.dropComponent(message.failure.typeId) }
            .then { it.refreshActiveValues() }
            .then { it.refreshCoreMarks() }
            .then { it.refreshSummary() }
            .then { it.showAddFailure(message.failure) }

        is Msg.AddFailed -> state.showAddFailure(message.failure)

        // ===== От экрана: запрос и закрытие =====

        Msg.RetryLookup -> state.startLooking(state.tokens)

        // Закрытие: с вопросом, если были правки (AWP-19, AWP-25).
        Msg.RequestClose -> when {
            state.isDirty -> state.askCloseConfirm()
            else -> state.close()
        }

        Msg.ConfirmClose -> state.close()

        Msg.Stay -> state.closeCloseConfirm()

        // ===== От экрана: карточки и значения =====

        // Удаление безвозвратно, флаг правок, сводка (AWP-15, AWP-19).
        is Msg.DeleteCard -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.deleteCard(message.lemma) }
            .then { it.markDirty() }
            .then { it.refreshSummary() }

        is Msg.DeleteLexeme -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.deleteLexeme(message.lemma, message.key) }
            .then { it.markDirty() }
            .then { it.refreshSummary() }

        is Msg.DeleteValue -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.deleteValue(message.ref) }
            .then { it.refreshActiveValues() }
            .then { it.markDirty() }
            .then { it.refreshExistsMarks() }
            .then { it.refreshCoreMarks() }
            .then { it.refreshSummary() }

        is Msg.SubmitText -> reduceSubmitText(state, message)

        is Msg.ResetText -> reduceResetText(state, message.ref)

        is Msg.SetOption -> reduceSetOption(state, message)

        // ===== От экрана: листы, группы, куда класть =====

        is Msg.OpenSheet -> state.openSheet(message.sheet)

        Msg.CloseSheet -> state.closeSheet()

        is Msg.UpdateDraft -> state.updateSheetDraft(message.text)

        // Выбор «куда класть» и групп — не правка: флага нет (AWP-19.6, 19.8).
        is Msg.SetCardTarget -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setCardTarget(message.lemma, message.target) }
            .then { it.closeSheet() }

        is Msg.SetCardGroup -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setCardGroup(message.lemma, message.group) }
            .then { it.closeSheet() }

        is Msg.SetCommonGroup -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setCommonGroup(message.groupId) }
            .then { it.closeSheet() }

        // ===== От экрана: показ =====

        Msg.ToggleComponentMatches -> state.toggleComponentMatchesExpanded()

        Msg.ToggleNotAdded -> state.toggleNotAddedExpanded()

        // ===== От экрана: запись (Ш4, В3) =====

        Msg.RequestAdd -> state.askAddConfirm()

        Msg.ConfirmAdd -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.closeAddConfirm() }
            .then { it.startAdding() }

        Msg.CancelAdd -> state.closeAddConfirm()

        // «Повторить» — заново без вопроса (AWP-26).
        Msg.RetryAdd -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.clearAddFailure() }
            .then { it.startAdding() }

        Msg.NoOperation -> state.noOp("noOperation")
    }

    /**
     * Правка из листа: обрезать, пустой параметр убрать, лист закрыть, пометки и
     * сводка. Флаг правок — только если текст сменился (AWP-19.9): условие по
     * стейту, инвариант спеки, тест редьюсера.
     */
    private fun reduceSubmitText(
        state: ScanCandidatesState,
        message: Msg.SubmitText,
    ): ScanCandidatesResult {
        val dirty = state.textChanged(message.ref, message.text)
        return state
            .begin<ScanCandidatesState, Effect>()
            .then { it.applyTextEdit(message.ref, message.text) }
            .then { it.dropEmptyTexts(message.ref) }
            .then { it.refreshActiveValues() }
            .then { it.closeSheet() }
            .then { if (dirty) it.markDirty() else it.begin() }
            .then { it.refreshExistsMarks() }
            .then { it.refreshCoreMarks() }
            .then { it.refreshSummary() }
    }

    /**
     * «Сбросить»: текст ответа обратно, лист закрыть, пометки и сводка. Флаг
     * правок — только если текст отличался от текста ответа (AWP-19.9): условие
     * по стейту, как у правки.
     */
    private fun reduceResetText(
        state: ScanCandidatesState,
        ref: ValueRefUi,
    ): ScanCandidatesResult {
        val dirty = state.textEdited(ref)
        return state
            .begin<ScanCandidatesState, Effect>()
            .then { it.resetText(ref) }
            .then { it.closeSheet() }
            .then { if (dirty) it.markDirty() else it.begin() }
            .then { it.refreshExistsMarks() }
            .then { it.refreshCoreMarks() }
            .then { it.refreshSummary() }
    }

    /**
     * Опция «Выбора» у лексемы-кандидата: зависимые значения пересчитать, лист
     * закрыть, флаг правок — только если опция сменилась (AWP-19.9). «Есть» и
     * ядро не трогает: «Выбор» — не ядро (ядро зависит только от лексемы).
     */
    private fun reduceSetOption(
        state: ScanCandidatesState,
        message: Msg.SetOption,
    ): ScanCandidatesResult {
        val dirty = state.optionChanged(message.ref, message.option)
        return state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setOption(message.ref, message.option) }
            .then { it.refreshActiveValues() }
            .then { it.closeSheet() }
            .then { if (dirty) it.markDirty() else it.begin() }
    }
}
