package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoDataKind
import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoFormError
import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.candidates.PhotoSheet
import me.apomazkin.candidates.PhotoTarget
import me.apomazkin.candidates.PhotoTextField
import me.apomazkin.candidates.PhotoWordStatus
import me.apomazkin.candidates.buildCandidateCards
import me.apomazkin.candidates.cardOf
import me.apomazkin.candidates.choose
import me.apomazkin.candidates.deletingCard
import me.apomazkin.candidates.deletingExample
import me.apomazkin.candidates.deletingLexeme
import me.apomazkin.candidates.droppingEmptyExamples
import me.apomazkin.candidates.errorResOf
import me.apomazkin.candidates.existingOf
import me.apomazkin.candidates.resolveMapping
import me.apomazkin.candidates.revalidate
import me.apomazkin.candidates.sameValuesOf
import me.apomazkin.candidates.summaryOf
import me.apomazkin.candidates.togglingCard
import me.apomazkin.candidates.withCoreMarks
import me.apomazkin.candidates.withExistsMarks
import me.apomazkin.candidates.withLemmaExistsMarks
import me.apomazkin.candidates.withPartOfSpeech
import me.apomazkin.candidates.withTextEdit
import me.apomazkin.candidates.withTextReset
import me.apomazkin.candidates.wordsBatchForTransfer
import me.apomazkin.candidates.wordsBatchForWord
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.wordTextKey
import me.apomazkin.logger.LexemeLogger

internal typealias CandidatesResult = ReducerResult<WordCardState, Effect>

/**
 * ###### Атомы диалога «значения из словаря» (WCL, конвенция §Reducer-
 * конвенция): один атом — один шаг над [WordCardState.candidates], каждый
 * логируется [logStep], атом без своих данных — [noOp]. Переходы над
 * карточками — общие функции модуля candidates, те же, что у окна снимка.
 * Цепочки и guard'ы — в ветках [WordCardReducer]. ######
 */
abstract class CandidatesAtoms(
    logger: LexemeLogger,
) : GroupBlockAtoms(logger) {

    /** ###### Ш1: ЗАПРОС И СБОРКА ###### */

    /**
     * Открыть диалог с новым номером запуска и уйти за леммами слова
     * (WCL-03, WCL-08.4). No-op без загруженного слова, без ключа сервера
     * и при уже открытом диалоге. Эффект запроса рождается здесь.
     */
    fun WordCardState.openCandidates(): CandidatesResult {
        val loaded = wordState as? WordState.Loaded ?: return noOp("openCandidates: word not loaded")
        if (!canAddFromDictionary) return noOp("openCandidates: no server access")
        if (candidates != null) return noOp("openCandidates: already open")
        val requestId = candidatesRun + 1
        logStep("openCandidates", "wordId" to loaded.id, "requestId" to requestId)
        val dialog = CandidatesState(wordId = loaded.id, text = loaded.value, dictionaryId = loaded.dictionaryId, requestId = requestId)
        return copy(candidatesRun = requestId, candidates = dialog) to setOf(
            DatasourceEffect.LookupCandidates(wordId = loaded.id, requestId = requestId),
        )
    }

    /**
     * Принять ответ сервера (WCL-04, WCL-11): есть лемма с лексемами — слова
     * в state и эффект формы с текстами лемм-форм (их слова в словаре —
     * получатели переноса, WCL-15); иначе текст причины. null — слова уже нет.
     */
    fun WordCardState.applyCandidatesLookup(outcome: PhotoLookupOutcome?): CandidatesResult {
        val dialog = candidates ?: return noOp("applyCandidatesLookup: no dialog")
        return when (outcome) {
            null -> showCandidatesError(R.string.candidates_word_missing)
            is PhotoLookupOutcome.Denied -> showCandidatesError(errorResOf(outcome.reason))
            PhotoLookupOutcome.Unavailable -> showCandidatesError(R.string.photo_words_error_server)
            is PhotoLookupOutcome.Words -> {
                val word = outcome.words.firstOrNull()
                val found = word != null && word.status == PhotoWordStatus.FOUND && word.lemmas.any { it.lexemes.isNotEmpty() }
                if (!found) {
                    showCandidatesError(R.string.candidates_not_found)
                } else {
                    val ownKey = wordTextKey(dialog.text)
                    val formLemmas = word!!.lemmas.filter { it.lexemes.isNotEmpty() && wordTextKey(it.text) != ownKey }.map { it.text }
                    logStep("startCandidatesForm", "wordId" to dialog.wordId, "lemmas" to word.lemmas.size, "formLemmas" to formLemmas.size)
                    copy(candidates = dialog.copy(words = outcome.words)) to setOf(
                        DatasourceEffect.LoadCandidatesForm(wordId = dialog.wordId, lemmaTexts = formLemmas, requestId = dialog.requestId),
                    )
                }
            }
        }
    }

    /** Текст причины вместо разделов (В1, WCL-11). Эффектов нет. */
    fun WordCardState.showCandidatesError(@StringRes errorRes: Int): CandidatesResult {
        logStep("showCandidatesError", "errorRes" to errorRes)
        return withCandidates { it.copy(phase = CandidatesPhase.ERROR, errorRes = errorRes, isAdding = false) }.begin()
    }

    /**
     * Принять форму (Ш1 → Ш2): разделы лемм — прямая первой, формы с
     * пометкой (WCL-04); соответствия — запомненные с умолчаниями (AWP-06).
     * Пометки и сводка — следующие атомы цепочки.
     */
    fun WordCardState.applyCandidatesForm(
        form: PhotoDictionaryForm,
        remembered: PhotoMapping?,
    ): CandidatesResult {
        val dialog = candidates ?: return noOp("applyCandidatesForm: no dialog")
        val word = dialog.words.firstOrNull() ?: return noOp("applyCandidatesForm: no words")
        val cards = buildCandidateCards(word, dialog.text, form)
        logStep("applyCandidatesForm", "components" to form.components.size, "cards" to cards.size)
        return copy(
            candidates = dialog.copy(
                phase = CandidatesPhase.READY,
                errorRes = null,
                form = form,
                mapping = form.resolveMapping(remembered),
                cards = cards,
            ),
        ).begin()
    }

    /** Форма не загрузилась (В1): словарь удалён или чтение упало. Эффектов нет. */
    fun WordCardState.showCandidatesFormError(error: PhotoFormError): CandidatesResult {
        logStep("showCandidatesFormError", "error" to error)
        val errorRes = when (error) {
            PhotoFormError.DICTIONARY_MISSING -> R.string.photo_add_dictionary_missing
            PhotoFormError.LOAD_FAILED -> R.string.photo_add_form_failed
        }
        return showCandidatesError(errorRes)
    }

    /** Пометки «есть» по лексемам слова диалога для всех разделов (WCL-06); слова лемм-форм не в счёт. No-op без формы. */
    fun WordCardState.refreshCandidatesExists(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesExists: no dialog")
        val form = dialog.form ?: return noOp("refreshCandidatesExists: no form")
        val cards = dialog.cards.withExistsMarks(dialog.ownLexemeKeys(form))
        logStep("refreshCandidatesExists", "exists" to cards.sumOf { card -> card.lexemes.count { it.exists } })
        return copy(candidates = dialog.copy(cards = cards)).begin()
    }

    /**
     * Пометки «есть в «correr»» у разделов лемм-форм и счёт совпадений под
     * выбором (WCL-15.4): кандидаты раздела плюс лексемы слова диалога,
     * которые уже есть у слов леммы. No-op без формы.
     */
    fun WordCardState.refreshCandidatesLemmaExists(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesLemmaExists: no dialog")
        val form = dialog.form ?: return noOp("refreshCandidatesLemmaExists: no form")
        val cards = dialog.cards.withLemmaExistsMarks(form)
        val sameValues = sameValuesOf(cards, form, ownKeys = dialog.ownLexemeKeys(form))
        logStep("refreshCandidatesLemmaExists", "existsInLemma" to cards.sumOf { card -> card.lexemes.count { it.existsInLemma } }, "sameValues" to sameValues)
        return copy(candidates = dialog.copy(cards = cards, sameValues = sameValues)).begin()
    }

    /** Пометки «не попадёт в ядро» (AWP-11). No-op без формы. */
    fun WordCardState.refreshCandidatesCore(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesCore: no dialog")
        val form = dialog.form ?: return noOp("refreshCandidatesCore: no form")
        val cards = dialog.cards.withCoreMarks(dialog.mapping, form)
        logStep("refreshCandidatesCore", "lacksCore" to cards.sumOf { card -> card.lexemes.count { it.lacksCore } })
        return copy(candidates = dialog.copy(cards = cards)).begin()
    }

    /**
     * Сводка кнопки «Добавить N значений» (AWP-13). Последний атом цепочки.
     * При переносе кандидаты, которые уже есть у слова-получателя, считаются
     * как «есть» — не пишутся (WCL-15.4).
     */
    fun WordCardState.refreshCandidatesSummary(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesSummary: no dialog")
        val summary = summaryOf(dialog.cardsForSummary(), dialog.mapping, dialog.form)
        logStep("refreshCandidatesSummary", "lexemes" to summary.lexemeCount, "existing" to summary.existingCount, "lacksCore" to summary.lacksCoreCount, "enabled" to summary.isEnabled)
        return copy(candidates = dialog.copy(summary = summary)).begin()
    }

    /** ###### Ш3: ДЕЙСТВИЯ ###### */

    fun WordCardState.toggleCandidateCard(lemma: String): CandidatesResult =
        cardsStep("toggleCandidateCard", "lemma" to lemma) { it.togglingCard(lemma) }

    fun WordCardState.deleteCandidateCard(lemma: String): CandidatesResult =
        cardsStep("deleteCandidateCard", "lemma" to lemma) { it.deletingCard(lemma) }

    fun WordCardState.deleteCandidateLexeme(
        lemma: String,
        key: String,
    ): CandidatesResult = cardsStep("deleteCandidateLexeme", "lemma" to lemma, "key" to key) { it.deletingLexeme(lemma, key) }

    fun WordCardState.deleteCandidateExample(
        lemma: String,
        key: String,
        index: Int,
    ): CandidatesResult = cardsStep("deleteCandidateExample", "lemma" to lemma, "key" to key, "index" to index) { it.deletingExample(lemma, key, index) }

    fun WordCardState.applyCandidateTextEdit(
        lemma: String,
        key: String,
        field: PhotoTextField,
        text: String,
    ): CandidatesResult = cardsStep("applyCandidateTextEdit", "lemma" to lemma, "key" to key, "field" to field, "length" to text.trim().length) {
        it.withTextEdit(lemma, key, field, text)
    }

    fun WordCardState.dropCandidateEmptyExamples(
        lemma: String,
        key: String,
    ): CandidatesResult = cardsStep("dropCandidateEmptyExamples", "lemma" to lemma, "key" to key) { it.droppingEmptyExamples(lemma, key) }

    fun WordCardState.resetCandidateText(
        lemma: String,
        key: String,
        field: PhotoTextField,
    ): CandidatesResult = cardsStep("resetCandidateText", "lemma" to lemma, "key" to key, "field" to field) { it.withTextReset(lemma, key, field) }

    fun WordCardState.setCandidatePartOfSpeech(
        lemma: String,
        key: String,
        optionId: Long?,
    ): CandidatesResult = cardsStep("setCandidatePartOfSpeech", "lemma" to lemma, "key" to key, "optionId" to optionId) { it.withPartOfSpeech(lemma, key, optionId) }

    /** Цель вида данных (AWP-09, AWP-28); запоминается для словаря эффектом (WCL-09.4). */
    fun WordCardState.setCandidatesMapping(
        kind: PhotoDataKind,
        target: PhotoTarget,
    ): CandidatesResult {
        val dialog = candidates ?: return noOp("setCandidatesMapping: no dialog")
        val mapping = dialog.mapping.choose(kind, target)
        logStep("setCandidatesMapping", "kind" to kind, "target" to target)
        return copy(candidates = dialog.copy(mapping = mapping)) to setOf(
            DatasourceEffect.SaveCandidatesMapping(dictionaryId = dialog.dictionaryId, mapping = mapping),
        )
    }

    /**
     * Что сделать со словом диалога при записи (WCL-15): переименовать в
     * лемму-форму или перенести в её слово; выбор один на диалог, повтор
     * снимает; у прямой леммы, неизвестной карточки и чужого слова — no-op.
     * Выбор — не правка: вопроса при закрытии не даёт.
     */
    fun WordCardState.toggleCandidatesLemmaWord(choice: LemmaWordChoice): CandidatesResult {
        val dialog = candidates ?: return noOp("toggleCandidatesLemmaWord: no dialog")
        val card = dialog.cards.cardOf(choice.lemma) ?: return noOp("toggleCandidatesLemmaWord: no card ${choice.lemma}")
        if (card.formNote == null) return noOp("toggleCandidatesLemmaWord: direct lemma ${choice.lemma}")
        if (choice is LemmaWordChoice.Transfer) {
            val known = dialog.form?.existingOf(choice.lemma).orEmpty().any { it.wordId == choice.wordId }
            if (!known) return noOp("toggleCandidatesLemmaWord: no word ${choice.wordId} for ${choice.lemma}")
        }
        val next = if (dialog.lemmaWordChoice == choice) null else choice
        logStep("toggleCandidatesLemmaWord", "choice" to next)
        return copy(candidates = dialog.copy(lemmaWordChoice = next)).begin()
    }

    fun WordCardState.toggleCandidatesMapping(): CandidatesResult {
        val dialog = candidates ?: return noOp("toggleCandidatesMapping: no dialog")
        logStep("toggleCandidatesMapping", "expanded" to !dialog.isMappingExpanded)
        return copy(candidates = dialog.copy(isMappingExpanded = !dialog.isMappingExpanded)).begin()
    }

    fun WordCardState.openCandidatesSheet(sheet: PhotoSheet): CandidatesResult {
        val dialog = candidates ?: return noOp("openCandidatesSheet: no dialog")
        logStep("openCandidatesSheet", "sheet" to sheet::class.simpleName)
        return copy(candidates = dialog.copy(sheet = sheet)).begin()
    }

    fun WordCardState.closeCandidatesSheet(): CandidatesResult {
        val dialog = candidates ?: return noOp("closeCandidatesSheet: no dialog")
        dialog.sheet ?: return noOp("closeCandidatesSheet: no sheet")
        logStep("closeCandidatesSheet")
        return copy(candidates = dialog.copy(sheet = null)).begin()
    }

    fun WordCardState.updateCandidatesDraft(text: String): CandidatesResult {
        val dialog = candidates ?: return noOp("updateCandidatesDraft: no dialog")
        val sheet = dialog.sheet as? PhotoSheet.TextEdit ?: return noOp("updateCandidatesDraft: no text edit")
        logStep("updateCandidatesDraft", "length" to text.length)
        return copy(candidates = dialog.copy(sheet = sheet.copy(draft = text))).begin()
    }

    /** Флаг «были изменения» (WCL-12): удаление, правка, смена части речи. */
    fun WordCardState.markCandidatesDirty(): CandidatesResult {
        val dialog = candidates ?: return noOp("markCandidatesDirty: no dialog")
        if (dialog.isDirty) return noOp("markCandidatesDirty: already dirty")
        logStep("markCandidatesDirty")
        return copy(candidates = dialog.copy(isDirty = true)).begin()
    }

    /** ###### Ш4, В3: ЗАПИСЬ ###### */

    /**
     * Запись без вопроса (WCL-10): пачка в слово диалога без группы — с
     * переименованием, если выбрано (WCL-15.2), — или перенос в слово леммы
     * (WCL-15.3); диалог неактивен.
     */
    fun WordCardState.startAddingCandidates(): CandidatesResult {
        val dialog = candidates ?: return noOp("startAddingCandidates: no dialog")
        val form = dialog.form ?: return noOp("startAddingCandidates: no form")
        val choice = dialog.lemmaWordChoice
        val batch = when (choice) {
            is LemmaWordChoice.Transfer -> wordsBatchForTransfer(
                targetWordId = choice.wordId,
                targetText = choice.lemma,
                absorbWordId = dialog.wordId,
                cards = dialog.cards,
                mapping = dialog.mapping,
                form = form,
            )
            else -> wordsBatchForWord(
                wordId = dialog.wordId,
                text = dialog.text,
                cards = dialog.cards,
                mapping = dialog.mapping,
                form = form,
                renameTo = (choice as? LemmaWordChoice.Rename)?.lemma,
            )
        }
        logStep("startAddingCandidates", "lexemes" to batch.words.sumOf { it.lexemes.size }, "choice" to choice)
        return copy(candidates = dialog.copy(isAdding = true, addFailure = null)) to setOf(
            DatasourceEffect.AddCandidates(
                batch = batch,
                wordId = dialog.wordId,
                requestId = dialog.requestId,
                isTransfer = choice is LemmaWordChoice.Transfer,
            ),
        )
    }

    fun WordCardState.showCandidatesAddFailure(failure: PhotoAddFailure): CandidatesResult {
        val dialog = candidates ?: return noOp("showCandidatesAddFailure: no dialog")
        logStep("showCandidatesAddFailure", "failure" to failure)
        return copy(candidates = dialog.copy(isAdding = false, addFailure = failure)).begin()
    }

    fun WordCardState.clearCandidatesAddFailure(): CandidatesResult {
        val dialog = candidates ?: return noOp("clearCandidatesAddFailure: no dialog")
        dialog.addFailure ?: return noOp("clearCandidatesAddFailure: no failure")
        logStep("clearCandidatesAddFailure")
        return copy(candidates = dialog.copy(addFailure = null)).begin()
    }

    /** Выключенный компонент (WCL-13.3): из формы прочь, его цели — к умолчаниям. */
    fun WordCardState.dropCandidatesComponent(typeId: Long): CandidatesResult {
        val dialog = candidates ?: return noOp("dropCandidatesComponent: no dialog")
        val form = dialog.form ?: return noOp("dropCandidatesComponent: no form")
        val narrowed = form.copy(components = form.components.filterNot { it.id == typeId })
        logStep("dropCandidatesComponent", "typeId" to typeId)
        return copy(candidates = dialog.copy(form = narrowed, mapping = narrowed.revalidate(dialog.mapping))).begin()
    }

    /** ###### В2: ЗАКРЫТИЕ ###### */

    fun WordCardState.askCandidatesClose(): CandidatesResult {
        val dialog = candidates ?: return noOp("askCandidatesClose: no dialog")
        logStep("askCandidatesClose")
        return copy(candidates = dialog.copy(isConfirmingClose = true)).begin()
    }

    fun WordCardState.closeCandidatesClose(): CandidatesResult {
        val dialog = candidates ?: return noOp("closeCandidatesClose: no dialog")
        if (!dialog.isConfirmingClose) return noOp("closeCandidatesClose: not asking")
        logStep("closeCandidatesClose")
        return copy(candidates = dialog.copy(isConfirmingClose = false)).begin()
    }

    /** Закрыть диалог: любым путём и после записи; номер запуска остаётся в [WordCardState.candidatesRun]. */
    fun WordCardState.hideCandidates(): CandidatesResult {
        val dialog = candidates ?: return noOp("hideCandidates: no dialog")
        logStep("hideCandidates", "requestId" to dialog.requestId)
        return copy(candidates = null).begin()
    }

    /** ###### ВНУТРЕННИЕ ХЕЛПЕРЫ ###### */

    private fun WordCardState.withCandidates(change: (CandidatesState) -> CandidatesState): WordCardState =
        candidates?.let { copy(candidates = change(it)) } ?: this

    /** Ключи текстов лексем самого слова диалога (WCL-06): слова лемм-форм в форме — не оно. */
    private fun CandidatesState.ownLexemeKeys(form: PhotoDictionaryForm): Set<String> =
        form.existingLemmas.filter { it.wordId == wordId }.flatMapTo(HashSet()) { it.lexemeTextKeys }

    /** Карточки для сводки: при переносе «есть в слове-получателе» = «есть» (WCL-15.4). */
    private fun CandidatesState.cardsForSummary(): List<me.apomazkin.candidates.PhotoCard> =
        if (lemmaWordChoice is LemmaWordChoice.Transfer) {
            cards.map { card -> card.copy(lexemes = card.lexemes.map { it.copy(exists = it.exists || it.existsInLemma) }) }
        } else {
            cards
        }

    /** Шаг над списком карточек: null от перехода — no-op с именем шага. */
    private fun WordCardState.cardsStep(
        step: String,
        vararg attrs: Pair<String, Any?>,
        change: (List<me.apomazkin.candidates.PhotoCard>) -> List<me.apomazkin.candidates.PhotoCard>?,
    ): CandidatesResult {
        val dialog = candidates ?: return noOp("$step: no dialog")
        val cards = change(dialog.cards) ?: return noOp("$step: nothing to change")
        logStep(step, *attrs)
        return copy(candidates = dialog.copy(cards = cards)).begin()
    }
}
