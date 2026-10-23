package me.apomazkin.scancandidates.mate

import androidx.annotation.StringRes
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardLexemeUiItem
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.buildLemmaCards
import me.apomazkin.candidates.componentMatchLinesOf
import me.apomazkin.candidates.deletingCard
import me.apomazkin.candidates.deletingLexeme
import me.apomazkin.candidates.deletingValue
import me.apomazkin.candidates.droppingEmptyTexts
import me.apomazkin.candidates.remarksOf
import me.apomazkin.candidates.summaryOf
import me.apomazkin.candidates.textOf
import me.apomazkin.candidates.withActiveValues
import me.apomazkin.candidates.withCardGroup
import me.apomazkin.candidates.withCardTarget
import me.apomazkin.candidates.withCoreMarks
import me.apomazkin.candidates.withExistsMarks
import me.apomazkin.candidates.withOption
import me.apomazkin.candidates.withTextEdit
import me.apomazkin.candidates.withTextReset
import me.apomazkin.candidates.withoutComponent
import me.apomazkin.candidates.withoutGroup
import me.apomazkin.candidates.withoutTarget
import me.apomazkin.candidates.wordsBatchOf
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ReducerLogging
import me.apomazkin.scancandidates.LogTags

typealias ScanCandidatesResult = ReducerResult<ScanCandidatesState, Effect>

/**
 * Атомы экрана лексем-кандидатов: один атом — один технический шаг, каждый
 * возвращает [ReducerResult] и логируется [logStep]; атом без своих данных —
 * [noOp] с причиной. Тяжёлое в лог идёт счётчиками. Условий в атомах нет: они в
 * ветках [ScanCandidatesReducer], которые ветвятся только по стейту. Переходы над
 * карточками — общие чистые функции `widget/candidates`, те же, что у диалога в
 * карточке слова. Редьюсер и тест атомов наследуют этот класс.
 */
abstract class ScanCandidatesAtoms(
    logger: LexemeLogger,
) : ReducerLogging(logger) {
    override val logTag: String = LogTags.SCAN

    // ===== Запуск: токены, запрос, ошибки =====

    /**
     * Токены в стейт, фаза «ищу слова», прежняя ошибка снята; эффект запроса
     * рождается здесь (WFP Ш6). Зовётся и после распознавания, и по «Повторить»
     * с токенами из стейта (Д53).
     * @param tokens токены как распознаны, с повторами.
     */
    fun ScanCandidatesState.startLooking(tokens: List<LookupToken>): ScanCandidatesResult {
        logStep("startLooking", "tokens" to tokens.size)
        val next = copy(
            phase = ScanPhase.LOOKING,
            tokens = tokens,
            errorRes = null,
            hasRetryButton = false,
        )
        return next to setOf(DatasourceEffect.LookupWords(tokens))
    }

    /** Фаза «слов нет», только «Закрыть» (WFP-19). Эффектов нет. */
    fun ScanCandidatesState.showNoWords(): ScanCandidatesResult {
        logStep("showNoWords")
        return copy(phase = ScanPhase.NO_WORDS).begin()
    }

    /**
     * Фаза «ошибка» с текстом, без «Повторить»: снимок не прочитался, отказ по
     * ключу или лимиту, форма не собралась (WFP-20, WFP-26.3, WFP-26.4).
     * Эффектов нет.
     */
    fun ScanCandidatesState.showError(
        @StringRes errorRes: Int,
    ): ScanCandidatesResult {
        logStep("showError", "errorRes" to errorRes)
        return copy(phase = ScanPhase.ERROR, errorRes = errorRes, hasRetryButton = false).begin()
    }

    /**
     * Фаза «ошибка» с текстом и «Повторить»: сети нет или сервер недоступен
     * (WFP-26.1, WFP-26.2, Д53). Эффектов нет.
     */
    fun ScanCandidatesState.showRetryableError(
        @StringRes errorRes: Int,
    ): ScanCandidatesResult {
        logStep("showRetryableError", "errorRes" to errorRes)
        return copy(phase = ScanPhase.ERROR, errorRes = errorRes, hasRetryButton = true).begin()
    }

    /**
     * Ответ сервера в стейт, фаза «собираю окно»; эффект формы рождается здесь —
     * ему нужны словарь и тексты лемм ответа без повторов (AWP Ш1, AWP-01).
     * @param found слова и рекомендации ответа со словарём снимка.
     */
    fun ScanCandidatesState.startFormLoading(found: Msg.WordsFound): ScanCandidatesResult {
        val lemmaTexts = found.words
            .flatMap { word -> word.lemmas.map { it.text } }
            .distinct()
        logStep(
            "startFormLoading",
            "dictionaryId" to found.dictionaryId,
            "words" to found.words.size,
            "lemmas" to lemmaTexts.size,
        )
        val next = copy(
            phase = ScanPhase.FORM_LOADING,
            dictionaryId = found.dictionaryId,
            words = found.words,
            recommendations = found.recommendations,
        )
        return next to setOf(DatasourceEffect.LoadForm(found.dictionaryId, lemmaTexts))
    }

    // ===== Окно: форма, карточки, пометки =====

    /**
     * Принять форму (Ш1 → Ш2): карточки и «не добавить» из слов ответа по
     * сопоставлению (AWP-04, AWP-12), общая группа — «Все» (AWP-08.7), блок «не
     * добавить» раскрыт, если карточек нет (AWP-12.3), фаза «окно». Замечания,
     * активность, пометки и сводка — следующие атомы цепочки. Эффектов нет.
     */
    fun ScanCandidatesState.applyForm(form: DictionaryFormUi): ScanCandidatesResult {
        val build = buildLemmaCards(words, form)
        logStep(
            "applyForm",
            "components" to form.components.size,
            "groups" to form.groups.size,
            "cards" to build.cards.size,
            "notAdded" to build.notAdded.size,
        )
        return copy(
            phase = ScanPhase.WINDOW,
            form = form,
            commonGroupId = null,
            cards = build.cards,
            notAdded = build.notAdded,
            isNotAddedExpanded = build.cards.isEmpty(),
        ).begin()
    }

    /**
     * Строки «Замечаний» из замечаний слов и рекомендаций ответа (AWP-30).
     * No-op без формы. Эффектов нет.
     */
    fun ScanCandidatesState.applyRemarks(): ScanCandidatesResult {
        val form = form ?: return noOp("applyRemarks: no form")
        val remarks = remarksOf(words, recommendations, form)
        logStep("applyRemarks", "remarks" to remarks.size)
        return copy(remarks = remarks).begin()
    }

    /**
     * Строки «Сопоставления» по сопоставлениям всех лексем ответа (AWP-31, Д41).
     * Правки пользователя их не меняют. No-op без формы. Эффектов нет.
     */
    fun ScanCandidatesState.applyComponentMatchLines(): ScanCandidatesResult {
        val form = form ?: return noOp("applyComponentMatchLines: no form")
        val lines = componentMatchLinesOf(words, form)
        logStep("applyComponentMatchLines", "lines" to lines.size)
        return copy(componentMatchLines = lines).begin()
    }

    /**
     * Активность значений по зависимостям компонентов (AWP-17.6, 17.7). Зовётся
     * до пометок: «есть» и «ядро» считаются по активным значениям. No-op без
     * формы. Эффектов нет.
     */
    fun ScanCandidatesState.refreshActiveValues(): ScanCandidatesResult {
        val form = form ?: return noOp("refreshActiveValues: no form")
        val cards = cards.withActiveValues(form)
        logStep("refreshActiveValues", "active" to cards.activeValueCount())
        return copy(cards = cards).begin()
    }

    /**
     * Пометки «есть» у лексем-кандидатов по ядрам и леммам словаря (AWP-07, Д42).
     * Зовётся после сборки и после правки текста. No-op без формы. Эффектов нет.
     */
    fun ScanCandidatesState.refreshExistsMarks(): ScanCandidatesResult {
        val form = form ?: return noOp("refreshExistsMarks: no form")
        val cards = cards.withExistsMarks(form)
        logStep("refreshExistsMarks", "exists" to cards.countLexemes { it.exists })
        return copy(cards = cards).begin()
    }

    /**
     * Пометки «не попадёт в ядро» (AWP-11). Зовётся после сборки, правки
     * текста и удаления значения. No-op без формы. Эффектов нет.
     */
    fun ScanCandidatesState.refreshCoreMarks(): ScanCandidatesResult {
        val form = form ?: return noOp("refreshCoreMarks: no form")
        val cards = cards.withCoreMarks(form)
        logStep("refreshCoreMarks", "lacksCore" to cards.countLexemes { it.lacksCore })
        return copy(cards = cards).begin()
    }

    /**
     * Сводка кнопки «добавить» (AWP-13). Последний атом каждой цепочки действия.
     * Эффектов нет.
     */
    fun ScanCandidatesState.refreshSummary(): ScanCandidatesResult {
        val summary = summaryOf(cards)
        logStep(
            "refreshSummary",
            "lexemes" to summary.lexemeCount,
            "words" to summary.wordCount,
            "existing" to summary.existingCount,
            "lacksCore" to summary.lacksCoreCount,
            "enabled" to summary.isEnabled,
        )
        return copy(summary = summary).begin()
    }

    // ===== Действия в окне =====

    /**
     * Удалить карточку — лемму со всеми лексемами-кандидатами, безвозвратно
     * (AWP-15.4). No-op, если карточки нет. Эффектов нет.
     */
    fun ScanCandidatesState.deleteCard(lemma: String): ScanCandidatesResult {
        return cardsStep("deleteCard", "lemma" to lemma) { it.deletingCard(lemma) }
    }

    /**
     * Удалить лексему-кандидата со всеми значениями (AWP-15.1). No-op, если её
     * нет. Эффектов нет.
     */
    fun ScanCandidatesState.deleteLexeme(
        lemma: String,
        key: String,
    ): ScanCandidatesResult {
        return cardsStep("deleteLexeme", "lemma" to lemma, "key" to key) {
            it.deletingLexeme(lemma, key)
        }
    }

    /**
     * Удалить одно значение компонента (AWP-15.2, 15.7): ядро без значений
     * остаётся пустым текстом (AWP-11.5), параметр без значений уходит. No-op
     * без формы и без значения. Эффектов нет.
     */
    fun ScanCandidatesState.deleteValue(ref: ValueRefUi): ScanCandidatesResult {
        val form = form ?: return noOp("deleteValue: no form")
        return cardsStep("deleteValue", *ref.attrs()) { it.deletingValue(ref, form) }
    }

    /**
     * Принять правку текста значения с обрезанными краями (AWP-16). Пустой текст
     * параметра убирает следующий атом цепочки ([dropEmptyTexts]). No-op, если
     * значения нет. Эффектов нет.
     */
    fun ScanCandidatesState.applyTextEdit(
        ref: ValueRefUi,
        text: String,
    ): ScanCandidatesResult {
        val length = text.trim().length
        return cardsStep("applyTextEdit", *ref.attrs(), "length" to length) {
            it.withTextEdit(ref, text)
        }
    }

    /**
     * Убрать стёртые тексты значения (AWP-16.5): у параметра пустой текст —
     * удалённый, у ядра единственный пустой остаётся (AWP-11.5). No-op без
     * формы, без значения и без пустых текстов. Эффектов нет.
     */
    fun ScanCandidatesState.dropEmptyTexts(ref: ValueRefUi): ScanCandidatesResult {
        val form = form ?: return noOp("dropEmptyTexts: no form")
        return cardsStep("dropEmptyTexts", *ref.attrs()) { it.droppingEmptyTexts(ref, form) }
    }

    /** Вернуть тексту значения текст ответа (AWP-16.6). No-op, если значения нет. Эффектов нет. */
    fun ScanCandidatesState.resetText(ref: ValueRefUi): ScanCandidatesResult {
        return cardsStep("resetText", *ref.attrs()) { it.withTextReset(ref) }
    }

    /**
     * Поставить опцию «Выбора» у одной лексемы-кандидата (AWP-17): у компонента с
     * одним значением замена, у «несколько» переключатель, null — «без значения».
     * Активность зависимых значений — следующий атом цепочки. No-op без формы и
     * без значения. Эффектов нет.
     */
    fun ScanCandidatesState.setOption(
        ref: ComponentRefUi,
        option: Long?,
    ): ScanCandidatesResult {
        val form = form ?: return noOp("setOption: no form")
        val attrs = arrayOf<Pair<String, Any?>>(
            "lemma" to ref.lemma,
            "key" to ref.key,
            "componentId" to ref.componentId,
            "option" to option,
        )
        return cardsStep("setOption", *attrs) { it.withOption(ref, option, form) }
    }

    /** Выбрать, куда класть лексемы карточки (AWP-18). No-op, если карточки нет. Эффектов нет. */
    fun ScanCandidatesState.setCardTarget(
        lemma: String,
        target: CardTargetUi,
    ): ScanCandidatesResult {
        return cardsStep("setCardTarget", "lemma" to lemma, "target" to target) {
            it.withCardTarget(lemma, target)
        }
    }

    /**
     * Поставить или снять свою группу карточки (AWP-08). No-op, если карточки
     * нет. Эффектов нет.
     */
    fun ScanCandidatesState.setCardGroup(
        lemma: String,
        group: CardOwnGroupUi,
    ): ScanCandidatesResult {
        return cardsStep("setCardGroup", "lemma" to lemma, "group" to group) {
            it.withCardGroup(lemma, group)
        }
    }

    /**
     * Сменить общую группу (AWP-08): группа словаря или «Все» (null). Не
     * запоминается (AWP-08.7). Эффектов нет.
     */
    fun ScanCandidatesState.setCommonGroup(groupId: Long?): ScanCandidatesResult {
        logStep("setCommonGroup", "groupId" to groupId)
        return copy(commonGroupId = groupId).begin()
    }

    /** Раскрыть или свернуть раздел «Сопоставление» (AWP-31.4). Эффектов нет. */
    fun ScanCandidatesState.toggleComponentMatchesExpanded(): ScanCandidatesResult {
        logStep("toggleComponentMatchesExpanded", "expanded" to !isComponentMatchesExpanded)
        return copy(isComponentMatchesExpanded = !isComponentMatchesExpanded).begin()
    }

    /** Раскрыть или свернуть блок «не добавить» (AWP-12.2). Эффектов нет. */
    fun ScanCandidatesState.toggleNotAddedExpanded(): ScanCandidatesResult {
        logStep("toggleNotAddedExpanded", "expanded" to !isNotAddedExpanded)
        return copy(isNotAddedExpanded = !isNotAddedExpanded).begin()
    }

    /**
     * Открыть лист, один за раз. У правки текста черновик — текущий текст
     * значения, «сбросить» есть, если текст отличается от текста ответа
     * (AWP-16.6, AWP-16.8). No-op, если значения под правку нет. Эффектов нет.
     */
    fun ScanCandidatesState.openSheet(sheet: CandidatesSheetUi): ScanCandidatesResult {
        val opened = if (sheet is CandidatesSheetUi.TextEdit) {
            val text = cards.textOf(sheet.ref) ?: return noOp("openSheet: no text ${sheet.ref}")
            sheet.copy(draft = text.text, canReset = text.isEdited)
        } else {
            sheet
        }
        logStep("openSheet", "sheet" to sheet::class.simpleName)
        return copy(sheet = opened).begin()
    }

    /** Закрыть лист. No-op, если листа нет. Эффектов нет. */
    fun ScanCandidatesState.closeSheet(): ScanCandidatesResult {
        sheet ?: return noOp("closeSheet: no sheet")
        logStep("closeSheet")
        return copy(sheet = null).begin()
    }

    /**
     * Принять ввод листа правки: «сбросить» есть, если ввод с обрезанными краями
     * отличается от текста ответа (AWP-16.6). Сравнивает атом, UI не сравнивает.
     * No-op, если открыт не лист правки. Эффектов нет.
     * @param text полный текущий текст поля.
     */
    fun ScanCandidatesState.updateSheetDraft(text: String): ScanCandidatesResult {
        val edit = sheet as? CandidatesSheetUi.TextEdit
            ?: return noOp("updateSheetDraft: no text edit")
        val original = cards.textOf(edit.ref)?.original
        logStep("updateSheetDraft", "length" to text.length)
        val next = edit.copy(draft = text, canReset = original != null && text.trim() != original)
        return copy(sheet = next).begin()
    }

    /**
     * Поднять флаг «были изменения»: удаление, правка текста или смена опции
     * (AWP-19). Выбор групп и «куда класть» его не ставит (AWP-19.6, 19.8).
     * Эффектов нет.
     */
    fun ScanCandidatesState.markDirty(): ScanCandidatesResult {
        logStep("markDirty")
        return copy(isDirty = true).begin()
    }

    // ===== Добавление: вопрос, запись, сбои =====

    /** Показать вопрос «Добавить N значений в M слов?» (AWP-13). Эффектов нет. */
    fun ScanCandidatesState.askAddConfirm(): ScanCandidatesResult {
        logStep("askAddConfirm", "lexemes" to summary.lexemeCount, "words" to summary.wordCount)
        return copy(isConfirmingAdd = true).begin()
    }

    /** Убрать вопрос о добавлении («да» или «отмена»). Эффектов нет. */
    fun ScanCandidatesState.closeAddConfirm(): ScanCandidatesResult {
        logStep("closeAddConfirm")
        return copy(isConfirmingAdd = false).begin()
    }

    /**
     * Начать запись (AWP-13.4, AWP-20): окно неактивно, прежний сбой снят, пакет
     * из содержимого окна уходит эффектом — он собирается из карточек, формы и
     * общей группы. No-op без формы.
     */
    fun ScanCandidatesState.startAdding(): ScanCandidatesResult {
        val form = form ?: return noOp("startAdding: no form")
        val batch = wordsBatchOf(cards, form, commonGroupId)
        logStep(
            "startAdding",
            "words" to batch.words.size,
            "lexemes" to batch.words.sumOf { it.lexemes.size },
        )
        return copy(isAdding = true, addFailure = null) to setOf(DatasourceEffect.AddWords(batch))
    }

    /**
     * Запись не удалась (В3, AWP-21): окно снова активно, причина — в
     * [ScanCandidatesState.addFailure]. Сбросы недействительного выбора —
     * отдельные атомы цепочки. Эффектов нет.
     */
    fun ScanCandidatesState.showAddFailure(failure: AddFailureUi): ScanCandidatesResult {
        logStep("showAddFailure", "failure" to failure)
        return copy(isAdding = false, addFailure = failure).begin()
    }

    /** Снять «не добавилось» перед повтором (AWP-26). No-op без сбоя. Эффектов нет. */
    fun ScanCandidatesState.clearAddFailure(): ScanCandidatesResult {
        addFailure ?: return noOp("clearAddFailure: no failure")
        logStep("clearAddFailure")
        return copy(addFailure = null).begin()
    }

    /**
     * Удалённая группа (AWP-21.3, 21.5): общая → «Все», своя у карточек → «как у
     * всех», из формы она уходит. Эффектов нет.
     * @param groupId id удалённой группы.
     */
    fun ScanCandidatesState.resetDeadGroup(groupId: Long): ScanCandidatesResult {
        logStep("resetDeadGroup", "groupId" to groupId)
        return copy(
            commonGroupId = commonGroupId?.takeIf { it != groupId },
            cards = cards.withoutGroup(groupId),
            form = form?.withoutDeadGroup(groupId),
        ).begin()
    }

    /**
     * Удалённая лемма словаря (AWP-21.4): «куда класть» у карточек с ней → «новое
     * слово», из формы она уходит. Эффектов нет.
     * @param wordId id удалённой леммы.
     */
    fun ScanCandidatesState.resetDeadTarget(wordId: Long): ScanCandidatesResult {
        logStep("resetDeadTarget", "wordId" to wordId)
        return copy(
            cards = cards.withoutTarget(wordId),
            form = form?.withoutDeadLemma(wordId),
        ).begin()
    }

    /**
     * Выключенный компонент (AWP-21.6): из формы уходит он сам, из лексем — его
     * значения. Активность, пометки и сводку пересчитывают следующие атомы
     * цепочки. No-op без формы. Эффектов нет.
     * @param typeId id компонента.
     */
    fun ScanCandidatesState.dropComponent(typeId: Long): ScanCandidatesResult {
        val form = form ?: return noOp("dropComponent: no form")
        logStep("dropComponent", "typeId" to typeId)
        return copy(
            form = form.copy(components = form.components.filterNot { it.id == typeId }),
            cards = cards.withoutComponent(typeId),
        ).begin()
    }

    // ===== Закрытие =====

    /** Показать вопрос «Закрыть без добавления?» (AWP-25). Эффектов нет. */
    fun ScanCandidatesState.askCloseConfirm(): ScanCandidatesResult {
        logStep("askCloseConfirm")
        return copy(isConfirmingClose = true).begin()
    }

    /** Убрать вопрос о закрытии («остаться»). Эффектов нет. */
    fun ScanCandidatesState.closeCloseConfirm(): ScanCandidatesResult {
        logStep("closeCloseConfirm")
        return copy(isConfirmingClose = false).begin()
    }

    /**
     * Назад на вкладку со счётом записанного (AWP-23): эффект навигации, стейт не
     * меняется.
     */
    fun ScanCandidatesState.closeWithResult(
        lexemeCount: Int,
        wordCount: Int,
    ): ScanCandidatesResult {
        logStep("closeWithResult", "lexemes" to lexemeCount, "words" to wordCount)
        return this to setOf(ScanNavigationEffect.CloseWithResult(lexemeCount, wordCount))
    }

    /** Назад на вкладку без результата: эффект навигации, стейт не меняется. */
    fun ScanCandidatesState.close(): ScanCandidatesResult {
        logStep("close")
        return this to setOf(NavigationEffect.Back)
    }

    // ===== Внутреннее: чистые помощники, не шаги, в лог не пишутся =====

    /** Шаг над списком карточек: null от перехода — no-op с именем шага. */
    private fun ScanCandidatesState.cardsStep(
        step: String,
        vararg attrs: Pair<String, Any?>,
        change: (List<LemmaCardUiItem>) -> List<LemmaCardUiItem>?,
    ): ScanCandidatesResult {
        val next = change(cards) ?: return noOp("$step: nothing to change")
        logStep(step, *attrs)
        return copy(cards = next).begin()
    }

    private fun ValueRefUi.attrs(): Array<Pair<String, Any?>> = arrayOf(
        "lemma" to lemma,
        "key" to key,
        "componentId" to componentId,
        "index" to index,
    )

    private fun List<LemmaCardUiItem>.countLexemes(predicate: (CardLexemeUiItem) -> Boolean): Int =
        sumOf { card -> card.lexemes.count(predicate) }

    private fun List<LemmaCardUiItem>.activeValueCount(): Int = this
        .flatMap { it.lexemes }
        .flatMap { it.values }
        .count { it.isActive }

    private fun DictionaryFormUi.withoutDeadGroup(groupId: Long): DictionaryFormUi =
        copy(groups = groups.filterNot { it.id == groupId })

    private fun DictionaryFormUi.withoutDeadLemma(wordId: Long): DictionaryFormUi =
        copy(existingLemmas = existingLemmas.filterNot { it.wordId == wordId })
}
