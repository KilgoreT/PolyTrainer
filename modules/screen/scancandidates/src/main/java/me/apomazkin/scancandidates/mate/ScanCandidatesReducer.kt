package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.begin
import io.github.kilgoret.mate.then
import me.apomazkin.logger.LexemeLogger

/**
 * Редьюсер экрана снимка: сообщение — видимая цепочка атомов
 * [ScanCandidatesAtoms]. Ветвится только по стейту, исходы юзкейсов не
 * разбирает: хендлер прислал сообщение на каждый случай. Условия — только в
 * ветке закрытия: пока идёт запись, крестик и «назад» не действуют
 * (AWP-51.12); есть слова с неразобранными плашками — вопрос (AWP-51).
 * Страховок от повторного нажатия нет: кнопки неактивны по флагам стейта.
 * Уход слова без плашек ([ScanCandidatesAtoms.removeEmptyWord]) стоит в
 * хвосте каждой цепочки, где плашка может исчезнуть (AWP-62).
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
        is Msg.LemmasFound,
        is Msg.FormLoaded,
        is Msg.LexemesFound,
        is Msg.LemmasReloaded,
        -> this::class.simpleName.orEmpty()
        else -> toString()
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun reduceMessage(
        state: ScanCandidatesState,
        message: Msg,
    ): ScanCandidatesResult = when (message) {
        // ===== От эффектов: снимок, запрос, форма =====

        is Msg.TextRecognized -> state.startAnalyzing(message.tokens)

        Msg.TextEmpty -> state.showNoWords()

        is Msg.RecognitionFailed -> state.showError(message.errorRes)

        is Msg.LemmasFound -> state.startFormLoading(message)

        Msg.LemmasEmpty -> state.showNoWords()

        is Msg.LemmasDenied -> state.showError(message.errorRes)

        is Msg.LemmasUnavailable -> state.showRetryableError(message.errorRes)

        is Msg.FormLoaded -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.applyForm(message.form) }
            .then { it.markInDictionary() }

        is Msg.FormFailed -> state.showError(message.errorRes)

        // ===== От эффектов: значения слова =====

        is Msg.LexemesFound -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.applyLexemes(message) }
            .then { it.refreshCardValues(message.text) }
            .then { it.refreshCardMarks(message.text) }

        is Msg.LexemesNotFound -> state.showCardNotFound(message.text, message.textRes)

        is Msg.LexemesDenied -> state.showCardError(message.text, message.errorRes)

        is Msg.LexemesUnavailable -> state.showCardRetryableError(message.text, message.errorRes)

        // ===== От эффектов: запись плашки и перечитка (AWP-20, AWP-63) =====

        is Msg.LexemeAdded -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.finishAdding(message.text, message.key) }
            .then { it.removeEmptyWord(message.text) }
            .then { it.startLemmasReload(message.text) }

        is Msg.WordCreated -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.finishAdding(message.text, message.key) }
            .then { it.retargetNewWordLexemes(message.text, message.wordId) }
            .then { it.removeEmptyWord(message.text) }
            .then { it.startLemmasReload(message.text) }

        is Msg.LemmasReloaded -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.applyReloadedLemmas(message.text, message.existing) }
            .then { it.markInDictionary() }
            .then { it.refreshCardMarks(message.text) }

        is Msg.LemmasReloadFailed -> state.noOp("lemmasReloadFailed: ${message.text}")

        // ===== От эффектов: сбои записи (AWP-21) =====

        is Msg.AddGroupGone -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.resetDeadGroup(message.failure.groupId) }
            .then { it.showAddFailure(message.text, message.key, message.failure) }

        is Msg.AddWordGone -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.resetDeadTarget(message.failure.wordId) }
            .then { it.refreshAllCards() }
            .then { it.showAddFailure(message.text, message.key, message.failure) }

        is Msg.AddComponentGone -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.dropComponent(message.failure.typeId) }
            .then { it.refreshAllCards() }
            .then { it.showAddFailure(message.text, message.key, message.failure) }

        is Msg.AddFailed -> state.showAddFailure(message.text, message.key, message.failure)

        // ===== От экрана: запрос, перевод, закрытие =====

        Msg.RetryLemmas -> state.startAnalyzing(state.tokens)

        Msg.ToggleTranslation -> state.toggleTranslation()

        // Пока идёт запись — ничего (AWP-51.12); есть что терять — вопрос (AWP-51).
        Msg.RequestClose -> when {
            state.isAdding -> state.noOp("requestClose: adding")
            state.lists.countPending() > 0 -> state.askCloseConfirm()
            else -> state.close()
        }

        Msg.ConfirmClose -> state.close()

        Msg.Stay -> state.closeCloseConfirm()

        // ===== От экрана: слово =====

        is Msg.LoadLexemes -> state.startLookup(message.text)

        is Msg.OpenSavedCard -> state.openSavedCard(message.text)

        is Msg.ToggleCard -> state.toggleCard(message.text)

        is Msg.RemoveWord -> state.removeWord(message.text)

        is Msg.ToggleComponentMatches -> state.toggleCardMatches(message.text)

        // ===== От экрана: плашка нейронки =====

        is Msg.AddLexeme -> state.startAddingLexeme(message.text, message.key)

        // «Повторить» — заново без вопроса (AWP-26).
        is Msg.RetryAdd -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.clearAddFailure(message.text, message.key) }
            .then { it.startAddingLexeme(message.text, message.key) }

        is Msg.DismissLexeme -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.dismissLexeme(message.text, message.key) }
            .then { it.refreshLemmaMarks(message.text) }
            .then { it.removeEmptyWord(message.text) }

        is Msg.SetCardTarget -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setLexemeTarget(message.lemma, message.key, message.target) }
            .then { it.closeSheet() }

        is Msg.SetCardGroup -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setLexemeGroup(message.lemma, message.key, message.group) }
            .then { it.closeSheet() }

        // ===== От экрана: правки плашки — все карточки леммы =====

        is Msg.DeleteValue -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.deleteValue(message.ref) }
            .then { it.refreshLemmaValues(message.ref.lemma) }
            .then { it.refreshLemmaMarks(message.ref.lemma) }
            .then { it.removeEmptyWord(message.ref.lemma) }

        is Msg.SubmitText -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.applyTextEdit(message.ref, message.text) }
            .then { it.dropEmptyTexts(message.ref) }
            .then { it.refreshLemmaValues(message.ref.lemma) }
            .then { it.closeSheet() }
            .then { it.refreshLemmaMarks(message.ref.lemma) }
            .then { it.removeEmptyWord(message.ref.lemma) }

        is Msg.ResetText -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.resetText(message.ref) }
            .then { it.closeSheet() }
            .then { it.refreshLemmaMarks(message.ref.lemma) }

        is Msg.SetOption -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setOption(message.ref, message.option) }
            .then { it.refreshLemmaValues(message.ref.lemma) }
            .then { it.closeSheet() }

        is Msg.UpdateDraft -> state.updateSheetDraft(message.text)

        // ===== От экрана: общая группа и листы =====

        is Msg.OpenSheet -> state.openSheet(message.sheet)

        Msg.CloseSheet -> state.closeSheet()

        is Msg.SetCommonGroup -> state
            .begin<ScanCandidatesState, Effect>()
            .then { it.setCommonGroup(message.groupId) }
            .then { it.closeSheet() }

        Msg.NoOperation -> state.noOp("noOperation")
    }
}
