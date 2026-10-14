package me.apomazkin.wordstab.logic

import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoFormError
import me.apomazkin.candidates.PhotoFormOutcome
import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoSheet
import me.apomazkin.candidates.addedMessage
import me.apomazkin.candidates.choose
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import io.github.kilgoret.mate.then
import io.github.kilgoret.mate.withEffect
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.logic.DatasourceEffect.CreateWord
import me.apomazkin.wordstab.logic.DatasourceEffect.LoadTermFlow
import me.apomazkin.wordstab.logic.DatasourceEffect.RecognizeWords
import me.apomazkin.wordstab.logic.DatasourceEffect.RemoveWords
import me.apomazkin.wordstab.logic.DatasourceEffect.UpdateWord
import me.apomazkin.wordstab.logic.photo.PhotoStateAtoms
import me.apomazkin.candidates.errorResOf
import me.apomazkin.candidates.lexemeOf
import me.apomazkin.candidates.originalOf
import me.apomazkin.candidates.textOf
import me.apomazkin.wordstab.logic.processor.processUiMessage
import me.apomazkin.wordstab.ui.WordsNavigationEffect

/**
 * Reducer вкладки «Слова». Диалог «слова с фото» и окно добавления (IS532)
 * — по §Reducer-конвенция: Msg → цепочка атомов [PhotoStateAtoms], guard'ы
 * и ветвление в ветках; остальные ветки — прежние экстеншны State.kt
 * (мигрируют по мере правок). Каждый Msg логируется именем или
 * `toString`; State целиком в лог не пишется (R-RP-007).
 */
class WordsTabReducer(
    logger: LexemeLogger,
) : PhotoStateAtoms(logger),
    MateReducer<WordsTabState, Msg, Effect> {
    override fun reduce(
        state: WordsTabState,
        message: Msg,
    ): ReducerResult<WordsTabState, Effect> {
        // Data-Msg — именем класса: их содержимое компактно пишет handler.
        logMessage(
            when (message) {
                is Msg.TermsLoaded, is Msg.TokensReady, is Msg.WordsLookedUp, is Msg.PhotoFormLoaded ->
                    message::class.simpleName.orEmpty()
                else -> message.toString()
            },
        )
        return reduceMessage(state, message)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun reduceMessage(
        state: WordsTabState,
        message: Msg,
    ): ReducerResult<WordsTabState, Effect> =
        when (message) {

            // IS496 Р3: тап по слову при открытой панели добавления —
            // намерение «добавить» сменилось на «открыть существующее»:
            // панель закрывается (ввод и фильтр сбрасываются), затем
            // навигация; возврат из карточки — к обычному списку.
            is Msg.OpenWordCard -> if (state.addWordDialogState.isOpen) {
                state
                    .hideAddWordDialog()
                    .toDefaultTermsFlow() to setOf(
                    WordsNavigationEffect.OpenWordCard(message.wordId)
                )
            } else {
                state to setOf(
                    WordsNavigationEffect.OpenWordCard(message.wordId)
                )
            }

            is Msg.SelectDictionary -> if (message.current == null) {
                // IS476: словарь отсутствует — переходим в пустое состояние таба.
                // Не запускаем LoadTermFlow (внутри был бы NPE на .id.toInt()).
                state.markNoDictionary() to emptySet()
            } else {
                // IS476: словарь появился (новый или переход null → non-null).
                // showLoading() перед LoadTermFlow — симметрично hideLoading() в Msg.TermsLoaded,
                // чтобы UI не моргал пустым WordListWidget с устаревшим termList.
                state.markDictionaryPresent().showLoading() to setOf(LoadTermFlow())
            }

            is Msg.TermsLoaded -> state
                .hideLoading()
                .appendTermsFlow(
                    pattern = message.pattern,
                    termsFlow = message.termList,
                ) to emptySet()

            is Msg.EnterSelectionMode -> state
                .modifySelectedSet(message.targetWord)
                .highlightWord(message.targetWord)
                .showActionMode() to setOf()

            is Msg.ExitSelectionMode -> state
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode() to setOf()

            is Msg.ToggleSelection -> state
                .modifySelectedSet(message.targetWord)
                .highlightWord(message.targetWord)
                .checkActionMode() to setOf()

            is Msg.OpenAddWordDialog -> state
                .showAddWordDialog(
                    wordValue = message.wordValue,
                    wordId = null,
                ) to setOf()

            is Msg.CloseAddWordDialog -> state
                .hideAddWordDialog()
                .toDefaultTermsFlow() to setOf()

            is Msg.OpenEditWordDialog -> state
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode()
                .showAddWordDialog(
                    wordId = message.wordId,
                    wordValue = message.wordValue,
                ) to setOf()

            is Msg.UpdateWordInput -> state.choose(
                check = { it.addWordDialogState.isOpen },
                yes = {
                    //TODO kilg 31.05.2025 01:30 добавить условие,
                    // что если "" уже есть, и паттерн "" -
                    // то не надо блять отправлять эффект создания потока
                    // https://github.com/KilgoreT/PolyTrainer/issues/377
                    it
                        .updateWordValue(message.value)
                        .retainDefaultAndCurrentFlow(message.value) to setOf(LoadTermFlow(message.value))
                },
                no = { it to emptySet() }
            )

            is Msg.CreateWord -> state
                .hideAddWordDialog()
                .toDefaultTermsFlow() to setOf(CreateWord(message.value))

            is Msg.UpdateWord -> state
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode()
                .hideAddWordDialog()
                .toDefaultTermsFlow() to setOf(
                UpdateWord(
                    wordId = message.wordId,
                    value = message.value,
                )
            )

            is Msg.OpenDeleteConfirmation -> state
                .showConfirmDeleteDialog(message.wordIds) to setOf()

            is Msg.CloseDeleteConfirmation -> state
                .hideConfirmDeleteDialog() to setOf()

            is Msg.RemoveWords -> state
                .hideConfirmDeleteDialog()
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode() to setOf(
                RemoveWords(wordSet = message.wordIds),
                UiEffect.ShowNotification(message = "Слово удалено")
            )

            // ===== IS527/IS529: запуск — выбор источника, снимок, запрос =====

            Msg.OpenPhotoSourceChooser -> state.showPhotoSourceChooser()

            // Принимается и при закрытом диалоге: система могла убить
            // процесс, пока была открыта камера, — снимок приходит в свежее
            // состояние и диалог открывается сразу на шаге распознавания.
            // Эффект несёт id нового запроса — он выдан в startRecognizing().
            is Msg.PhotoPicked -> state
                .startRecognizing()
                .let { (next, effects) ->
                    next to effects + RecognizeWords(uri = message.uri, requestId = next.photoWordsState.requestId)
                }

            // Возврат без снимка — тот же запуск, номер не меняется (WFT-02.2).
            // Отмена в камере после смерти процесса не открывает диалог сама (WFT-64.3).
            Msg.PhotoPickCancelled -> when {
                !state.photoWordsState.isOpen -> state.noOp("photoPickCancelled: dialog closed")
                else -> state.returnToPhotoSourceChooser()
            }

            // Результат чужого или закрытого запуска отбрасывается (WFT-62).
            // Токены есть — на сервер (WFT-16.2); нет — «слов не найдено» (WFT-60.1).
            is Msg.TokensReady -> when {
                !state.isCurrentPhotoRequest(message.requestId) -> state.noOp("tokensReady: foreign run ${message.requestId}")
                message.tokens.isEmpty() -> state.showNoPhotoWords()
                else -> state
                    .startLooking()
                    .withEffect(DatasourceEffect.LookupWords(tokens = message.tokens, requestId = message.requestId))
            }

            // Ответ сервера своего запуска: слова → сборка окна (Ш1), пусто
            // (WFT-60.2), отказ по причине (WFT-63) или «сервер не ответил».
            is Msg.WordsLookedUp -> when {
                !state.isCurrentPhotoRequest(message.requestId) -> state.noOp("wordsLookedUp: foreign run ${message.requestId}")
                else -> when (val outcome = message.outcome) {
                    is PhotoLookupOutcome.Words -> when {
                        outcome.words.isEmpty() -> state.showNoPhotoWords()
                        else -> state.startFormLoading(dictionaryId = outcome.dictionaryId, words = outcome.words)
                    }
                    is PhotoLookupOutcome.Denied -> state.showPhotoError(errorResOf(outcome.reason))
                    PhotoLookupOutcome.Unavailable -> state.showPhotoError(R.string.photo_words_error_server)
                }
            }

            is Msg.RecognitionFailed -> when {
                !state.isCurrentPhotoRequest(message.requestId) -> state.noOp("recognitionFailed: foreign run ${message.requestId}")
                else -> state.showPhotoError(message.errorRes)
            }

            Msg.ClosePhotoWords -> state.hidePhotoWords()

            // ===== IS532: окно добавления =====

            // Ш1 → Ш2 / В1: форма своего запуска (AWP-24.2).
            is Msg.PhotoFormLoaded -> when {
                !state.isCurrentPhotoRequest(message.requestId) -> state.noOp("photoFormLoaded: foreign run ${message.requestId}")
                else -> when (val outcome = message.outcome) {
                    is PhotoFormOutcome.Loaded -> state
                        .begin<WordsTabState, Effect>()
                        .then { it.applyForm(form = outcome.form, remembered = outcome.remembered) }
                        .then { it.refreshExistsMarks() }
                        .then { it.refreshCoreMarks() }
                        .then { it.refreshSummary() }
                    PhotoFormOutcome.DictionaryMissing -> state.showFormError(PhotoFormError.DICTIONARY_MISSING)
                    is PhotoFormOutcome.Failed -> state.showFormError(PhotoFormError.LOAD_FAILED)
                }
            }

            is Msg.TogglePhotoCard -> state.toggleCard(message.lemma)

            // Удаление — безвозвратно, флаг изменений, сводка (AWP-15, AWP-19).
            is Msg.DeletePhotoCard -> state
                .begin<WordsTabState, Effect>()
                .then { it.deleteCard(message.lemma) }
                .then { it.markDirty() }
                .then { it.refreshSummary() }

            is Msg.DeletePhotoLexeme -> state
                .begin<WordsTabState, Effect>()
                .then { it.deleteLexeme(message.lemma, message.key) }
                .then { it.markDirty() }
                .then { it.refreshSummary() }

            is Msg.DeletePhotoExample -> state
                .begin<WordsTabState, Effect>()
                .then { it.deleteExample(message.lemma, message.key, message.index) }
                .then { it.markDirty() }
                .then { it.refreshCoreMarks() }
                .then { it.refreshSummary() }

            is Msg.OpenPhotoSheet -> when {
                state.photoWordsState.isAdding -> state.noOp("openPhotoSheet: adding")
                else -> state.openSheet(message.sheet)
            }

            Msg.ClosePhotoSheet -> state.closeSheet()

            is Msg.UpdatePhotoDraft -> state.updateSheetDraft(message.text)

            // Правка текста из листа: обрезать, пустой пример — удалить,
            // «есть» и «ядро» пересчитать (AWP-16, AWP-07.7, AWP-11.5).
            // Флаг изменений — только если текст и правда сменился (AWP-19.9).
            Msg.SubmitPhotoText -> when (val sheet = state.photoWordsState.sheet) {
                is PhotoSheet.TextEdit -> {
                    val changed = state.photoWordsState.cards.lexemeOf(sheet.lemma, sheet.key)?.textOf(sheet.field) != sheet.draft.trim()
                    state
                        .begin<WordsTabState, Effect>()
                        .then { it.applyTextEdit(sheet.lemma, sheet.key, sheet.field, sheet.draft) }
                        .then { it.dropEmptyExamples(sheet.lemma, sheet.key) }
                        .then { it.closeSheet() }
                        .then { if (changed) it.markDirty() else it.begin() }
                        .then { it.refreshExistsMarks() }
                        .then { it.refreshCoreMarks() }
                        .then { it.refreshSummary() }
                }
                else -> state.noOp("submitPhotoText: no text edit sheet")
            }

            is Msg.ResetPhotoText -> {
                val lexeme = state.photoWordsState.cards.lexemeOf(message.lemma, message.key)
                val changed = lexeme != null && lexeme.textOf(message.field) != lexeme.originalOf(message.field)
                state
                    .begin<WordsTabState, Effect>()
                    .then { it.resetText(message.lemma, message.key, message.field) }
                    .then { it.closeSheet() }
                    .then { if (changed) it.markDirty() else it.begin() }
                    .then { it.refreshExistsMarks() }
                    .then { it.refreshCoreMarks() }
                    .then { it.refreshSummary() }
            }

            // Часть речи — изменение (вопрос при закрытии, AWP-19.7), если и правда сменилась (AWP-19.9); ядро не трогает.
            is Msg.SetPhotoPartOfSpeech -> {
                val changed = state.photoWordsState.cards.lexemeOf(message.lemma, message.key)?.partOfSpeechOptionId != message.optionId
                state
                    .begin<WordsTabState, Effect>()
                    .then { it.setPartOfSpeech(message.lemma, message.key, message.optionId) }
                    .then { it.closeSheet() }
                    .then { if (changed) it.markDirty() else it.begin() }
            }

            // Выборы групп, «куда класть» и соответствий — без флага изменений (AWP-19.6, 19.8).
            is Msg.SetPhotoCardTarget -> state
                .begin<WordsTabState, Effect>()
                .then { it.setCardTarget(message.lemma, message.target) }
                .then { it.closeSheet() }

            is Msg.SetPhotoCardGroup -> state
                .begin<WordsTabState, Effect>()
                .then { it.setCardGroup(message.lemma, message.group) }
                .then { it.closeSheet() }

            is Msg.SetPhotoCommonGroup -> state
                .begin<WordsTabState, Effect>()
                .then { it.setCommonGroup(message.groupId) }
                .then { it.closeSheet() }

            is Msg.SetPhotoMapping -> state
                .begin<WordsTabState, Effect>()
                .then { it.setMappingTarget(message.kind, message.target) }
                .then { it.closeSheet() }
                .then { it.refreshCoreMarks() }
                .then { it.refreshSummary() }

            Msg.TogglePhotoMapping -> state.toggleMappingExpanded()

            Msg.TogglePhotoNotAdded -> state.toggleNotAddedExpanded()

            // Ш4: кнопка → вопрос; неактивная кнопка и повторные нажатия не действуют (AWP-02, AWP-13).
            Msg.RequestPhotoAdd -> when {
                state.photoWordsState.isAdding -> state.noOp("requestPhotoAdd: already adding")
                !state.photoWordsState.summary.isEnabled -> state.noOp("requestPhotoAdd: button disabled")
                state.photoWordsState.isConfirmingAdd -> state.noOp("requestPhotoAdd: already asking")
                else -> state.askAddConfirm()
            }

            Msg.ConfirmPhotoAdd -> when {
                !state.photoWordsState.isConfirmingAdd -> state.noOp("confirmPhotoAdd: not asking")
                state.photoWordsState.isAdding -> state.noOp("confirmPhotoAdd: already adding")
                else -> state
                    .begin<WordsTabState, Effect>()
                    .then { it.closeAddConfirm() }
                    .then { it.startAdding() }
            }

            Msg.CancelPhotoAdd -> state.closeAddConfirm()

            // В3: «повторить» — заново без вопроса (AWP-26).
            Msg.RetryPhotoAdd -> when {
                state.photoWordsState.isAdding -> state.noOp("retryPhotoAdd: already adding")
                state.photoWordsState.addFailure == null -> state.noOp("retryPhotoAdd: nothing failed")
                !state.photoWordsState.summary.isEnabled -> state.noOp("retryPhotoAdd: button disabled")
                else -> state
                    .begin<WordsTabState, Effect>()
                    .then { it.clearAddFailure() }
                    .then { it.startAdding() }
            }

            // Ш5: записано — окно закрыто, сообщение со счётом (AWP-23).
            is Msg.PhotoAddFinished -> when {
                !state.isCurrentPhotoRequest(message.requestId) -> state.noOp("photoAddFinished: foreign run ${message.requestId}")
                else -> state
                    .hidePhotoWords()
                    .withEffect(UiEffect.ShowNotification(message = addedMessage(message.lexemeCount, message.wordCount)))
            }

            // В3: отказ до первой вставки — сброс недействительного выбора по причине (AWP-21.3…21.6).
            is Msg.PhotoAddRejected -> when {
                !state.isCurrentPhotoRequest(message.requestId) -> state.noOp("photoAddRejected: foreign run ${message.requestId}")
                else -> when (val failure = message.failure) {
                    is PhotoAddFailure.GroupMissing -> state
                        .begin<WordsTabState, Effect>()
                        .then { it.resetDeadGroup(failure.groupId) }
                        .then { it.showAddFailure(failure) }
                    is PhotoAddFailure.WordMissing -> state
                        .begin<WordsTabState, Effect>()
                        .then { it.resetDeadTarget(failure.wordId) }
                        .then { it.showAddFailure(failure) }
                    is PhotoAddFailure.TypeUnavailable -> state
                        .begin<WordsTabState, Effect>()
                        .then { it.dropComponent(failure.typeId) }
                        .then { it.refreshCoreMarks() }
                        .then { it.refreshSummary() }
                        .then { it.showAddFailure(failure) }
                    PhotoAddFailure.NoCoreValue, PhotoAddFailure.DictionaryMissing, PhotoAddFailure.Failed ->
                        state.showAddFailure(failure)
                }
            }

            // Ш2 → В2: закрытие — сразу, с вопросом после изменений, никак во время записи (AWP-19, AWP-24.3).
            Msg.RequestClosePhotoWords -> when {
                !state.photoWordsState.isOpen -> state.noOp("requestClosePhotoWords: dialog closed")
                state.photoWordsState.isAdding -> state.noOp("requestClosePhotoWords: adding")
                state.photoWordsState.isConfirmingClose -> state.noOp("requestClosePhotoWords: already asking")
                state.photoWordsState.isDirty -> state.askCloseConfirm()
                else -> state.hidePhotoWords()
            }

            Msg.ConfirmClosePhotoWords -> when {
                !state.photoWordsState.isConfirmingClose -> state.noOp("confirmClosePhotoWords: not asking")
                else -> state.hidePhotoWords()
            }

            Msg.StayInPhotoWords -> state.closeCloseConfirm()

            is UiMsg -> processUiMessage(state, message)
            Msg.NoOperation -> state to emptySet()
        }
}
