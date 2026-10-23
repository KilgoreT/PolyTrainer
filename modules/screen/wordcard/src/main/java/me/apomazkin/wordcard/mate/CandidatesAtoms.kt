package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.buildCandidateCards
import me.apomazkin.candidates.cardOf
import me.apomazkin.candidates.componentMatchLinesOf
import me.apomazkin.candidates.deletingCard
import me.apomazkin.candidates.deletingLexeme
import me.apomazkin.candidates.deletingValue
import me.apomazkin.candidates.droppingEmptyTexts
import me.apomazkin.candidates.existingOf
import me.apomazkin.candidates.remarksOf
import me.apomazkin.candidates.sameValuesOf
import me.apomazkin.candidates.summaryOf
import me.apomazkin.candidates.textOf
import me.apomazkin.candidates.withActiveValues
import me.apomazkin.candidates.withCoreMarks
import me.apomazkin.candidates.withExistsMarks
import me.apomazkin.candidates.withLemmaExistsMarks
import me.apomazkin.candidates.withOption
import me.apomazkin.candidates.withTextEdit
import me.apomazkin.candidates.withTextReset
import me.apomazkin.candidates.withoutComponent
import me.apomazkin.candidates.wordsBatchForTransfer
import me.apomazkin.candidates.wordsBatchForWord
import me.apomazkin.lexeme.wordTextKey
import me.apomazkin.logger.LexemeLogger

internal typealias CandidatesResult = ReducerResult<WordCardState, Effect>

/**
 * ###### Атомы диалога лексем-кандидатов (WCL, конвенция §Reducer-
 * конвенция): один атом — один шаг над [WordCardState.candidates], каждый
 * логируется [logStep], атом без своих данных — [noOp]. Переходы над
 * карточками и сборка пометок — общие функции модуля candidates, те же, что
 * у окна снимка. Цепочки и проверка номера запуска — в ветках
 * [WordCardReducer]. ######
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
        val dialog = CandidatesState(
            wordId = loaded.id,
            text = loaded.value,
            dictionaryId = loaded.dictionaryId,
            requestId = requestId,
        )
        return copy(candidatesRun = requestId, candidates = dialog) to setOf(
            DatasourceEffect.LookupCandidates(wordId = loaded.id, requestId = requestId),
        )
    }

    /**
     * Принять слово ответа (WCL-04): слово и рекомендации в state и эффект
     * формы с текстами лемм-форм — лемм с лексемами, чей текст не текст
     * слова диалога; их слова в словаре — получатели переноса (WCL-15).
     */
    fun WordCardState.startCandidatesForm(found: Msg.CandidatesFound): CandidatesResult {
        val dialog = candidates ?: return noOp("startCandidatesForm: no dialog")
        val ownKey = wordTextKey(dialog.text)
        val formLemmas = found
            .word
            .lemmas
            .filter { it.lexemes.isNotEmpty() && wordTextKey(it.text) != ownKey }
            .map { it.text }
        logStep(
            "startCandidatesForm",
            "wordId" to dialog.wordId,
            "lemmas" to found.word.lemmas.size,
            "formLemmas" to formLemmas.size,
            "recommendations" to found.recommendations.size,
        )
        return copy(
            candidates = dialog.copy(
                words = listOf(found.word),
                recommendations = found.recommendations,
            ),
        ) to setOf(
            DatasourceEffect.LoadCandidatesForm(
                wordId = dialog.wordId,
                lemmaTexts = formLemmas,
                requestId = dialog.requestId,
            ),
        )
    }

    /** Текст причины вместо разделов, «Повторить» нет (В1, WCL-11). Эффектов нет. */
    fun WordCardState.showCandidatesError(@StringRes errorRes: Int): CandidatesResult =
        showError("showCandidatesError", errorRes, hasRetryButton = false)

    /** Текст причины вместо разделов и «Повторить» рядом с «Закрыть» (WCL-11.9). Эффектов нет. */
    fun WordCardState.showCandidatesRetryableError(@StringRes errorRes: Int): CandidatesResult =
        showError("showCandidatesRetryableError", errorRes, hasRetryButton = true)

    /**
     * «Повторить» (WCL-11.9): запрос заново с тем же словом под новым номером
     * запуска — ответ прежнего запуска отбрасывается; признак загрузки.
     */
    fun WordCardState.retryCandidatesLookup(): CandidatesResult {
        val dialog = candidates ?: return noOp("retryCandidatesLookup: no dialog")
        val requestId = candidatesRun + 1
        logStep("retryCandidatesLookup", "wordId" to dialog.wordId, "requestId" to requestId)
        val loading = dialog.copy(
            requestId = requestId,
            phase = CandidatesPhase.LOADING,
            errorRes = null,
            hasRetryButton = false,
        )
        return copy(candidatesRun = requestId, candidates = loading) to setOf(
            DatasourceEffect.LookupCandidates(wordId = dialog.wordId, requestId = requestId),
        )
    }

    /**
     * Принять форму (Ш1 → Ш2): разделы лемм — прямая первой, формы с
     * пометкой (WCL-04). Остальное — следующие атомы цепочки.
     */
    fun WordCardState.applyCandidatesForm(form: DictionaryFormUi): CandidatesResult {
        val dialog = candidates ?: return noOp("applyCandidatesForm: no dialog")
        val word = dialog.words.firstOrNull() ?: return noOp("applyCandidatesForm: no words")
        val cards = buildCandidateCards(word, dialog.text, form)
        logStep("applyCandidatesForm", "components" to form.components.size, "cards" to cards.size)
        return copy(
            candidates = dialog.copy(
                phase = CandidatesPhase.READY,
                errorRes = null,
                form = form,
                cards = cards,
            ),
        ).begin()
    }

    /**
     * Строки «Замечаний» из замечаний слова и рекомендаций ответа (WCL-16,
     * AWP-30). No-op без формы.
     */
    fun WordCardState.applyCandidatesRemarks(): CandidatesResult {
        val dialog = candidates ?: return noOp("applyCandidatesRemarks: no dialog")
        val form = dialog.form ?: return noOp("applyCandidatesRemarks: no form")
        val remarks = remarksOf(dialog.words, dialog.recommendations, form)
        logStep("applyCandidatesRemarks", "remarks" to remarks.size)
        return copy(candidates = dialog.copy(remarks = remarks)).begin()
    }

    /**
     * Строки «Сопоставления» по сопоставлениям слова, без повторов (WCL-17,
     * AWP-31). No-op без формы.
     */
    fun WordCardState.applyCandidatesComponentMatchLines(): CandidatesResult {
        val dialog = candidates ?: return noOp("applyCandidatesComponentMatchLines: no dialog")
        val form = dialog.form ?: return noOp("applyCandidatesComponentMatchLines: no form")
        val lines = componentMatchLinesOf(dialog.words, form)
        logStep("applyCandidatesComponentMatchLines", "lines" to lines.size)
        return copy(candidates = dialog.copy(componentMatchLines = lines)).begin()
    }

    /** Активность значений по зависимостям компонентов формы (AWP-17.6). No-op без формы. */
    fun WordCardState.refreshCandidatesActiveValues(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesActiveValues: no dialog")
        val form = dialog.form ?: return noOp("refreshCandidatesActiveValues: no form")
        val cards = dialog.cards.withActiveValues(form)
        val values = cards.flatMap { card -> card.lexemes.flatMap { it.values } }
        logStep("refreshCandidatesActiveValues", "inactive" to values.count { !it.isActive })
        return copy(candidates = dialog.copy(cards = cards)).begin()
    }

    /**
     * Пометки «есть» по лексемам слова диалога для всех разделов (WCL-06);
     * слова лемм-форм не в счёт. No-op без формы.
     */
    fun WordCardState.refreshCandidatesExists(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesExists: no dialog")
        val form = dialog.form ?: return noOp("refreshCandidatesExists: no form")
        val cards = dialog.cards.withExistsMarks(dialog.ownLexemeKeys(form), form)
        val exists = cards.flatMap { it.lexemes }.count { it.exists }
        logStep("refreshCandidatesExists", "exists" to exists)
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
        logStep(
            "refreshCandidatesLemmaExists",
            "existsInLemma" to cards.sumOf { card -> card.lexemes.count { it.existsInLemma } },
            "sameValues" to sameValues,
        )
        return copy(candidates = dialog.copy(cards = cards, sameValues = sameValues)).begin()
    }

    /** Пометки «не попадёт в ядро» (AWP-11). No-op без формы. */
    fun WordCardState.refreshCandidatesCore(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesCore: no dialog")
        val form = dialog.form ?: return noOp("refreshCandidatesCore: no form")
        val cards = dialog.cards.withCoreMarks(form)
        val lacksCore = cards.flatMap { it.lexemes }.count { it.lacksCore }
        logStep("refreshCandidatesCore", "lacksCore" to lacksCore)
        return copy(candidates = dialog.copy(cards = cards)).begin()
    }

    /**
     * Сводка кнопки «Добавить N значений» (AWP-13). Последний атом цепочки.
     * При переносе кандидаты, которые уже есть у слова-получателя, считаются
     * как «есть» — не пишутся (WCL-15.4).
     */
    fun WordCardState.refreshCandidatesSummary(): CandidatesResult {
        val dialog = candidates ?: return noOp("refreshCandidatesSummary: no dialog")
        val summary = summaryOf(dialog.cardsForSummary())
        logStep(
            "refreshCandidatesSummary",
            "lexemes" to summary.lexemeCount,
            "existing" to summary.existingCount,
            "lacksCore" to summary.lacksCoreCount,
            "enabled" to summary.isEnabled,
        )
        return copy(candidates = dialog.copy(summary = summary)).begin()
    }

    /** ###### Ш3: ДЕЙСТВИЯ ###### */

    fun WordCardState.deleteCandidateCard(lemma: String): CandidatesResult =
        cardsStep("deleteCandidateCard", "lemma" to lemma) { it.deletingCard(lemma) }

    fun WordCardState.deleteCandidateLexeme(
        lemma: String,
        key: String,
    ): CandidatesResult =
        cardsStep("deleteCandidateLexeme", "lemma" to lemma, "key" to key) {
            it.deletingLexeme(lemma, key)
        }

    /**
     * Одно значение компонента прочь (AWP-15.2, 15.7); ядро без значений
     * остаётся пустым (AWP-11.5).
     */
    fun WordCardState.deleteCandidateValue(ref: ValueRefUi): CandidatesResult =
        formStep("deleteCandidateValue", "ref" to ref) { cards, form ->
            cards.deletingValue(ref, form)
        }

    /** Правка текста значения с обрезанными краями (AWP-16). */
    fun WordCardState.applyCandidateTextEdit(
        ref: ValueRefUi,
        text: String,
    ): CandidatesResult =
        cardsStep("applyCandidateTextEdit", "ref" to ref, "length" to text.trim().length) {
            it.withTextEdit(ref, text)
        }

    /**
     * Стёртые тексты прочь: у параметра пустой текст — удалённый, у ядра
     * единственный остаётся (AWP-16.5).
     */
    fun WordCardState.dropCandidateEmptyTexts(ref: ValueRefUi): CandidatesResult =
        formStep("dropCandidateEmptyTexts", "ref" to ref) { cards, form ->
            cards.droppingEmptyTexts(ref, form)
        }

    /** Текст ответа обратно (AWP-16.6). */
    fun WordCardState.resetCandidateText(ref: ValueRefUi): CandidatesResult =
        cardsStep("resetCandidateText", "ref" to ref) { it.withTextReset(ref) }

    /** Опция «Выбора» у одной лексемы-кандидата; null — «без значения» (AWP-17). */
    fun WordCardState.setCandidateOption(
        ref: ComponentRefUi,
        option: Long?,
    ): CandidatesResult =
        formStep("setCandidateOption", "ref" to ref, "option" to option) { cards, form ->
            cards.withOption(ref, option, form)
        }

    /**
     * Что сделать со словом диалога при записи (WCL-15): переименовать в
     * лемму-форму или перенести в её слово; выбор один на диалог, повтор
     * снимает; у прямой леммы, неизвестной карточки и чужого слова — no-op.
     * Выбор — не правка: вопроса при закрытии не даёт.
     */
    fun WordCardState.toggleCandidatesLemmaWord(choice: LemmaWordChoice): CandidatesResult {
        val dialog = candidates ?: return noOp("toggleCandidatesLemmaWord: no dialog")
        val card = dialog.cards.cardOf(choice.lemma)
            ?: return noOp("toggleCandidatesLemmaWord: no card ${choice.lemma}")
        if (card.formNote == null) {
            return noOp("toggleCandidatesLemmaWord: direct lemma ${choice.lemma}")
        }
        if (choice is LemmaWordChoice.Transfer) {
            val words = dialog.form?.existingOf(choice.lemma).orEmpty()
            if (words.none { it.wordId == choice.wordId }) {
                return noOp("toggleCandidatesLemmaWord: no word ${choice.wordId}")
            }
        }
        val next = if (dialog.lemmaWordChoice == choice) null else choice
        logStep("toggleCandidatesLemmaWord", "choice" to next)
        return copy(candidates = dialog.copy(lemmaWordChoice = next)).begin()
    }

    /**
     * Раскрыть или свернуть «Сопоставление» (AWP-31.4); вопроса при закрытии
     * не даёт (WCL-12.6).
     */
    fun WordCardState.toggleCandidatesComponentMatches(): CandidatesResult {
        val dialog = candidates ?: return noOp("toggleCandidatesComponentMatches: no dialog")
        val expanded = !dialog.isComponentMatchesExpanded
        logStep("toggleCandidatesComponentMatches", "expanded" to expanded)
        return copy(candidates = dialog.copy(isComponentMatchesExpanded = expanded)).begin()
    }

    /**
     * Открыть лист. У правки текста черновик — текущий текст значения, а
     * «сбросить» доступен, если он отличается от текста ответа (AWP-16.8):
     * считает атом, UI тексты не сравнивает.
     */
    fun WordCardState.openCandidatesSheet(sheet: CandidatesSheetUi): CandidatesResult {
        val dialog = candidates ?: return noOp("openCandidatesSheet: no dialog")
        val opened = when (sheet) {
            is CandidatesSheetUi.TextEdit -> {
                val item = dialog.cards.textOf(sheet.ref)
                    ?: return noOp("openCandidatesSheet: no text ${sheet.ref}")
                sheet.copy(draft = item.text, canReset = item.isEdited)
            }
            else -> sheet
        }
        logStep("openCandidatesSheet", "sheet" to opened::class.simpleName)
        return copy(candidates = dialog.copy(sheet = opened)).begin()
    }

    fun WordCardState.closeCandidatesSheet(): CandidatesResult {
        val dialog = candidates ?: return noOp("closeCandidatesSheet: no dialog")
        dialog.sheet ?: return noOp("closeCandidatesSheet: no sheet")
        logStep("closeCandidatesSheet")
        return copy(candidates = dialog.copy(sheet = null)).begin()
    }

    /**
     * Ввод в листе правки: «сбросить» доступен, пока ввод с обрезанными краями
     * отличается от текста ответа — «Сохранить» всё равно обрежет (AWP-16.6).
     */
    fun WordCardState.updateCandidatesDraft(text: String): CandidatesResult {
        val dialog = candidates ?: return noOp("updateCandidatesDraft: no dialog")
        val sheet = dialog.sheet as? CandidatesSheetUi.TextEdit
            ?: return noOp("updateCandidatesDraft: no text edit")
        val original = dialog.cards.textOf(sheet.ref)?.original
        val canReset = original != null && text.trim() != original
        logStep("updateCandidatesDraft", "length" to text.length, "canReset" to canReset)
        return copy(
            candidates = dialog.copy(sheet = sheet.copy(draft = text, canReset = canReset)),
        ).begin()
    }

    /** Флаг «были изменения» (WCL-12): удаление, правка, смена опции. */
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
                form = form,
            )
            else -> wordsBatchForWord(
                wordId = dialog.wordId,
                text = dialog.text,
                cards = dialog.cards,
                form = form,
                renameTo = (choice as? LemmaWordChoice.Rename)?.lemma,
            )
        }
        logStep(
            "startAddingCandidates",
            "lexemes" to batch.words.sumOf { it.lexemes.size },
            "choice" to choice,
        )
        return copy(candidates = dialog.copy(isAdding = true, addFailure = null)) to setOf(
            DatasourceEffect.AddCandidates(
                batch = batch,
                wordId = dialog.wordId,
                requestId = dialog.requestId,
                isTransfer = choice is LemmaWordChoice.Transfer,
            ),
        )
    }

    fun WordCardState.showCandidatesAddFailure(failure: AddFailureUi): CandidatesResult {
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

    /**
     * Выключенный компонент (WCL-13.3, AWP-21.6): из формы прочь и из значений
     * всех лексем-кандидатов.
     */
    fun WordCardState.dropCandidatesComponent(typeId: Long): CandidatesResult {
        val dialog = candidates ?: return noOp("dropCandidatesComponent: no dialog")
        val form = dialog.form ?: return noOp("dropCandidatesComponent: no form")
        val narrowed = form.copy(components = form.components.filterNot { it.id == typeId })
        val cards = dialog.cards.withoutComponent(typeId)
        logStep("dropCandidatesComponent", "typeId" to typeId)
        return copy(candidates = dialog.copy(form = narrowed, cards = cards)).begin()
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

    /**
     * Закрыть диалог: любым путём и после записи; номер запуска остаётся в
     * [WordCardState.candidatesRun].
     */
    fun WordCardState.hideCandidates(): CandidatesResult {
        val dialog = candidates ?: return noOp("hideCandidates: no dialog")
        logStep("hideCandidates", "requestId" to dialog.requestId)
        return copy(candidates = null).begin()
    }

    /** ###### ВНУТРЕННИЕ ХЕЛПЕРЫ ###### */

    /**
     * Фаза «ошибка»: текст причины вместо разделов, запись не идёт;
     * «Повторить» по [hasRetryButton].
     */
    private fun WordCardState.showError(
        step: String,
        @StringRes errorRes: Int,
        hasRetryButton: Boolean,
    ): CandidatesResult {
        val dialog = candidates ?: return noOp("$step: no dialog")
        logStep(step, "errorRes" to errorRes)
        return copy(
            candidates = dialog.copy(
                phase = CandidatesPhase.ERROR,
                errorRes = errorRes,
                hasRetryButton = hasRetryButton,
                isAdding = false,
            ),
        ).begin()
    }

    /** Ключи текстов лексем самого слова диалога (WCL-06): слова лемм-форм в форме — не оно. */
    private fun CandidatesState.ownLexemeKeys(form: DictionaryFormUi): Set<String> = form
        .existingLemmas
        .filter { it.wordId == wordId }
        .flatMapTo(HashSet()) { it.lexemeTextKeys }

    /** Карточки для сводки: при переносе «есть в слове-получателе» = «есть» (WCL-15.4). */
    private fun CandidatesState.cardsForSummary(): List<LemmaCardUiItem> =
        if (lemmaWordChoice is LemmaWordChoice.Transfer) {
            cards.map { card ->
                val lexemes = card.lexemes.map { it.copy(exists = it.exists || it.existsInLemma) }
                card.copy(lexemes = lexemes)
            }
        } else {
            cards
        }

    /** Шаг над списком карточек: null от перехода — no-op с именем шага. */
    private fun WordCardState.cardsStep(
        step: String,
        vararg attrs: Pair<String, Any?>,
        change: (List<LemmaCardUiItem>) -> List<LemmaCardUiItem>?,
    ): CandidatesResult {
        val dialog = candidates ?: return noOp("$step: no dialog")
        val cards = change(dialog.cards) ?: return noOp("$step: nothing to change")
        logStep(step, *attrs)
        return copy(candidates = dialog.copy(cards = cards)).begin()
    }

    /** То же, когда переход решает по форме словаря («ядро или параметр», «несколько»). */
    private fun WordCardState.formStep(
        step: String,
        vararg attrs: Pair<String, Any?>,
        change: (List<LemmaCardUiItem>, DictionaryFormUi) -> List<LemmaCardUiItem>?,
    ): CandidatesResult {
        val dialog = candidates ?: return noOp("$step: no dialog")
        val form = dialog.form ?: return noOp("$step: no form")
        val cards = change(dialog.cards, form) ?: return noOp("$step: nothing to change")
        logStep(step, *attrs)
        return copy(candidates = dialog.copy(cards = cards)).begin()
    }
}
