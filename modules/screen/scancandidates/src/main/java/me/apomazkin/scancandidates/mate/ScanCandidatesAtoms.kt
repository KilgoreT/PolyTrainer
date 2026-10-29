package me.apomazkin.scancandidates.mate

import androidx.annotation.StringRes
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.componentMatchLinesOf
import me.apomazkin.candidates.deletingLexeme
import me.apomazkin.candidates.deletingValue
import me.apomazkin.candidates.droppingEmptyTexts
import me.apomazkin.candidates.emptyLemmaCard
import me.apomazkin.candidates.existingOf
import me.apomazkin.candidates.lemmaCardOf
import me.apomazkin.candidates.lexemeKeysOf
import me.apomazkin.candidates.remarksOf
import me.apomazkin.candidates.savedLexemesOf
import me.apomazkin.candidates.textOf
import me.apomazkin.candidates.withActiveValues
import me.apomazkin.candidates.withCardGroup
import me.apomazkin.candidates.withCardTarget
import me.apomazkin.candidates.withCoreMarks
import me.apomazkin.candidates.withExistingLemmas
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
 * Атомы экрана снимка: один атом — один технический шаг, каждый возвращает
 * [ReducerResult] и логируется [logStep]; атом без своих данных — [noOp] с
 * причиной. Тяжёлое в лог идёт счётчиками. Условий в атомах нет: они в ветках
 * [ScanCandidatesReducer], которые ветвятся только по стейту. Переходы над
 * карточкой — общие чистые функции `widget/candidates`, те же, что у диалога в
 * карточке слова; атом только адресует карточку — словом или леммой — и пишет
 * лог. Редьюсер и тест атомов наследуют этот класс.
 */
@Suppress("TooManyFunctions", "LargeClass")
abstract class ScanCandidatesAtoms(
    logger: LexemeLogger,
) : ReducerLogging(logger) {
    override val logTag: String = LogTags.SCAN

    // ===== Запуск: токены, запрос, ошибки =====

    /**
     * Токены в стейт, фаза «анализирую текст», прежняя ошибка снята; эффект
     * запроса рождается здесь (TFP Ш6). Зовётся и после распознавания, и по
     * «Повторить» с токенами из стейта (TFP-26).
     * @param tokens токены как распознаны, с повторами.
     */
    fun ScanCandidatesState.startAnalyzing(tokens: List<LookupToken>): ScanCandidatesResult {
        logStep("startAnalyzing", "tokens" to tokens.size)
        val next = copy(
            phase = ScanPhase.ANALYZING,
            tokens = tokens,
            errorRes = null,
            hasRetryButton = false,
        )
        return next to setOf(DatasourceEffect.ExtractLemmas(tokens))
    }

    /** Фаза «слов нет», только «Закрыть» (TFP-19, AWP-54). Эффектов нет. */
    fun ScanCandidatesState.showNoWords(): ScanCandidatesResult {
        logStep("showNoWords")
        return copy(phase = ScanPhase.NO_WORDS).begin()
    }

    /**
     * Фаза «ошибка» с текстом, без «Повторить»: снимок не прочитался, отказ по
     * ключу, лимиту, размеру или языку, форма не собралась (TFP-26.3, TFP-26.4,
     * AWP-24, AWP-53). Эффектов нет.
     */
    fun ScanCandidatesState.showError(
        @StringRes errorRes: Int,
    ): ScanCandidatesResult {
        logStep("showError", "errorRes" to errorRes)
        return copy(phase = ScanPhase.ERROR, errorRes = errorRes, hasRetryButton = false).begin()
    }

    /**
     * Фаза «ошибка» с текстом и «Повторить»: сети нет, сервер не ответил или
     * нейронка недоступна (TFP-26.1, TFP-26.2). Эффектов нет.
     */
    fun ScanCandidatesState.showRetryableError(
        @StringRes errorRes: Int,
    ): ScanCandidatesResult {
        logStep("showRetryableError", "errorRes" to errorRes)
        return copy(phase = ScanPhase.ERROR, errorRes = errorRes, hasRetryButton = true).begin()
    }

    /**
     * Ответ сервера в стейт, фаза «собираю окно»; эффект формы рождается здесь —
     * ему нужны словарь и тексты всех слов списков без повторов: по ним форма
     * несёт леммы словаря для пометки «в словаре» (AWP Ш4, AWP-01, AWP-39).
     * @param found текст, перевод и списки ответа со словарём снимка.
     */
    fun ScanCandidatesState.startFormLoading(found: Msg.LemmasFound): ScanCandidatesResult {
        val lemmaTexts = found.lists.allTexts().distinct()
        logStep(
            "startFormLoading",
            "dictionaryId" to found.dictionaryId,
            "words" to lemmaTexts.size,
        )
        val next = copy(
            phase = ScanPhase.FORM_LOADING,
            dictionaryId = found.dictionaryId,
            text = found.text,
            translation = found.translation,
            lists = found.lists,
        )
        return next to setOf(DatasourceEffect.LoadForm(found.dictionaryId, lemmaTexts))
    }

    // ===== Окно: форма, пометки, перевод =====

    /**
     * Принять форму (Ш4 → Ш5): форма в стейт, общая группа — «Все» (AWP-08.7),
     * перевод свёрнут (AWP-41.2), фаза «окно». Пометки «в словаре» — следующий
     * атом цепочки. Эффектов нет.
     */
    fun ScanCandidatesState.applyForm(form: DictionaryFormUi): ScanCandidatesResult {
        logStep(
            "applyForm",
            "groups" to form.groups.size,
            "existing" to form.existingLemmas.size,
        )
        return copy(
            phase = ScanPhase.WINDOW,
            form = form,
            commonGroupId = null,
            isTranslationExpanded = false,
        ).begin()
    }

    /**
     * Пометка «в словаре» у слова, чей текст совпал с леммой словаря без учёта
     * регистра, краёв и формы Unicode (AWP-39). No-op без формы. Эффектов нет.
     */
    fun ScanCandidatesState.markInDictionary(): ScanCandidatesResult {
        val form = form ?: return noOp("markInDictionary: no form")
        val marked = lists.withDictionaryMarks(form.existingLemmas)
        logStep("markInDictionary", "inDictionary" to marked.countInDictionary())
        return copy(lists = marked).begin()
    }

    /** Блок перевода раскрыт или свёрнут (AWP-41). Эффектов нет. */
    fun ScanCandidatesState.toggleTranslation(): ScanCandidatesResult {
        logStep("toggleTranslation", "expanded" to !isTranslationExpanded)
        return copy(isTranslationExpanded = !isTranslationExpanded).begin()
    }

    // ===== Карточка под словом: сохранённые, запрос, ответ =====

    /**
     * Карточка слова «в словаре» без сервера (Ш6 Б, AWP-46): лемма словаря по
     * умолчанию самая новая (AWP-18.3), её сохранённые лексемы, «куда класть»
     * и своя группа «как у всех»; раскрыта. No-op без формы и без слова.
     * Эффектов нет.
     */
    fun ScanCandidatesState.openSavedCard(text: String): ScanCandidatesResult {
        val form = form ?: return noOp("openSavedCard: no form")
        lists.wordOf(text) ?: return noOp("openSavedCard: no word $text")
        val lemma = emptyLemmaCard(text, form)
        logStep("openSavedCard", "word" to text, "existing" to lemma.existingCount)
        val card = WordCardUi(
            phase = CardPhase.SAVED,
            lemma = lemma,
            saved = form.savedLexemesOf(lemma.target),
        )
        return copy(lists = lists.updateWord(text) { it.copy(card = card, isExpanded = true) }).begin()
    }

    /**
     * Карточка «загрузка» раскрыта (Ш6 А, Ш8, AWP-57): прежняя ошибка снята,
     * лемма и сохранённые как были или пустая лемма у нового слова; эффект
     * запроса — лемма слова с текстом и переводом снимка у словаря снимка
     * (LFL-103, AWP-01). No-op без формы, без словаря и без слова.
     */
    fun ScanCandidatesState.startLookup(text: String): ScanCandidatesResult {
        val form = form ?: return noOp("startLookup: no form")
        val dictionaryId = dictionaryId ?: return noOp("startLookup: no dictionary")
        val word = lists.wordOf(text) ?: return noOp("startLookup: no word $text")
        val lemma = word.card?.lemma ?: emptyLemmaCard(text, form)
        logStep("startLookup", "word" to text)
        val card = WordCardUi(
            phase = CardPhase.LOADING,
            lemma = lemma,
            saved = word.card?.saved.orEmpty(),
        )
        val next = copy(lists = lists.updateWord(text) { it.copy(card = card, isExpanded = true) })
        return next to setOf(
            DatasourceEffect.LookupLemma(
                word = text,
                lemma = text,
                dictionaryId = dictionaryId,
                text = this.text,
                translation = translation,
            ),
        )
    }

    /**
     * Ответ в карточку (Ш6 → карточка, AWP-43): леммы словаря с текстом леммы
     * ответа, которых в форме не было, — в форму (AWP-43.7); лемма ответа с
     * лексемами-кандидатами по сопоставлению, первая «в тексте»; «куда класть»
     * и своя группа — как были у карточки; «Замечания» и «Сопоставление» по
     * ответу (AWP-29, AWP-30, AWP-31). Активность, пометки и «Добавить» —
     * следующие атомы цепочки. Слова уже нет — ничего (AWP-48.4). No-op без
     * формы. Эффектов нет.
     */
    fun ScanCandidatesState.applyLexemes(found: Msg.LexemesFound): ScanCandidatesResult {
        val form = form?.withExistingLemmas(found.existing) ?: return noOp("applyLexemes: no form")
        val word = lists.wordOf(found.text) ?: return noOp("applyLexemes: no word ${found.text}")
        val prior = word.card?.lemma ?: emptyLemmaCard(found.text, form)
        val lemma = lemmaCardOf(found.word, prior, form)
        logStep(
            "applyLexemes",
            "word" to found.text,
            "lemma" to lemma.lemma,
            "lexemes" to lemma.lexemes.size,
        )
        val card = WordCardUi(
            phase = CardPhase.CANDIDATES,
            lemma = lemma,
            saved = form.savedLexemesOf(lemma.target),
            remarks = remarksOf(listOf(found.word), found.recommendations, form),
            componentMatchLines = componentMatchLinesOf(listOf(found.word), form),
        )
        return copy(
            form = form,
            lists = lists.updateWord(found.text) { it.copy(card = card) },
        ).begin()
    }

    /**
     * Активность значений карточки слова по зависимостям компонентов
     * (AWP-17.6). Зовётся до пометок: «уже в словаре» и ядро считаются по
     * активным значениям. No-op без формы и без карточки. Эффектов нет.
     */
    fun ScanCandidatesState.refreshCardValues(text: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshCardValues: no form")
        val card = lists.wordOf(text)?.card ?: return noOp("refreshCardValues: no card $text")
        val lemma = card.lemma.withActiveValues(form)
        logStep("refreshCardValues", "word" to text, "active" to lemma.activeValueCount())
        return copy(lists = lists.updateWord(text) { it.copy(card = card.copy(lemma = lemma)) }).begin()
    }

    /**
     * Пометки карточки слова: «уже в словаре» по ключам ядер леммы из «куда
     * класть» (AWP-50), «не попадёт в ядро» (AWP-11), «Добавить» активна,
     * когда есть лексема без «уже в словаре» и нет лексем без ядра (AWP-47.2,
     * 47.3). No-op без формы и без карточки. Эффектов нет.
     */
    fun ScanCandidatesState.refreshCardMarks(text: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshCardMarks: no form")
        val card = lists.wordOf(text)?.card ?: return noOp("refreshCardMarks: no card $text")
        val marked = card.withMarks(form)
        logStep(
            "refreshCardMarks",
            "word" to text,
            "exists" to marked.lemma.lexemes.count { it.exists },
            "lacksCore" to marked.lemma.lexemes.count { it.lacksCore },
            "canAdd" to marked.canAdd,
        )
        return copy(lists = lists.updateWord(text) { it.copy(card = marked) }).begin()
    }

    /** Карточка «не найдено» с текстом: без «Добавить», «Удалить» есть (AWP-55). No-op без карточки. */
    fun ScanCandidatesState.showCardNotFound(
        text: String,
        @StringRes textRes: Int,
    ): ScanCandidatesResult = showCardMessage("showCardNotFound", text, CardPhase.NOT_FOUND, textRes, hasRetryButton = false)

    /** Карточка «ошибка» без «Повторить»: отказ по ключу, лимиту, размеру или языку (AWP-61.3, 61.4, 61.6, 61.8). */
    fun ScanCandidatesState.showCardError(
        text: String,
        @StringRes errorRes: Int,
    ): ScanCandidatesResult = showCardMessage("showCardError", text, CardPhase.ERROR, errorRes, hasRetryButton = false)

    /** Карточка «ошибка» с «Повторить»: сети нет, сервер не ответил, нейронка недоступна (AWP-61.1, 61.2, 61.5). */
    fun ScanCandidatesState.showCardRetryableError(
        text: String,
        @StringRes errorRes: Int,
    ): ScanCandidatesResult = showCardMessage("showCardRetryableError", text, CardPhase.ERROR, errorRes, hasRetryButton = true)

    /** Карточка свёрнута или раскрыта такой, какой была (AWP-42). No-op без слова и без карточки. Эффектов нет. */
    fun ScanCandidatesState.toggleCard(text: String): ScanCandidatesResult {
        val word = lists.wordOf(text) ?: return noOp("toggleCard: no word $text")
        word.card ?: return noOp("toggleCard: no card $text")
        logStep("toggleCard", "word" to text, "expanded" to !word.isExpanded)
        return copy(lists = lists.updateWord(text) { it.copy(isExpanded = !it.isExpanded) }).begin()
    }

    /**
     * «Удалить» у слова: строки прочь из всех списков без записи; ответ,
     * который придёт позже, некуда класть (AWP-48). No-op без слова. Эффектов
     * нет.
     */
    fun ScanCandidatesState.deleteWord(text: String): ScanCandidatesResult {
        lists.wordOf(text) ?: return noOp("deleteWord: no word $text")
        logStep("deleteWord", "word" to text)
        return copy(lists = lists.removeWord(text)).begin()
    }

    /** Раздел «Сопоставление» карточки слова раскрыт или свёрнут (AWP-31.4). No-op без карточки. Эффектов нет. */
    fun ScanCandidatesState.toggleCardMatches(text: String): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("toggleCardMatches: no card $text")
        val expanded = !card.isComponentMatchesExpanded
        logStep("toggleCardMatches", "word" to text, "expanded" to expanded)
        return copy(
            lists = lists.updateWord(text) { it.copy(card = card.copy(isComponentMatchesExpanded = expanded)) },
        ).begin()
    }

    // ===== Правки: адрес — лемма карточки, ложатся во все карточки леммы =====

    /** Лексема-кандидат со всеми значениями прочь (AWP-15.1). No-op, если её нет. Эффектов нет. */
    fun ScanCandidatesState.deleteLexeme(
        lemma: String,
        key: String,
    ): ScanCandidatesResult = lemmaStep("deleteLexeme", lemma, "key" to key) { card ->
        listOf(card).deletingLexeme(lemma, key)?.single()
    }

    /**
     * Одно значение компонента прочь (AWP-15.2, 15.7); ушёл последний текст
     * ядра — уходит вся лексема (AWP-11.5). No-op без формы и без значения.
     * Эффектов нет.
     */
    fun ScanCandidatesState.deleteValue(ref: ValueRefUi): ScanCandidatesResult {
        val form = form ?: return noOp("deleteValue: no form")
        return lemmaStep("deleteValue", ref.lemma, *ref.attrs()) { card ->
            listOf(card).deletingValue(ref, form)?.single()
        }
    }

    /**
     * Правка текста значения с обрезанными краями (AWP-16). Стёртый текст
     * убирает следующий атом цепочки ([dropEmptyTexts]). No-op, если значения
     * нет. Эффектов нет.
     */
    fun ScanCandidatesState.applyTextEdit(
        ref: ValueRefUi,
        text: String,
    ): ScanCandidatesResult = lemmaStep("applyTextEdit", ref.lemma, *ref.attrs(), "length" to text.trim().length) { card ->
        listOf(card).withTextEdit(ref, text)?.single()
    }

    /**
     * Стёртые тексты значения прочь (AWP-16.5): без текстов уходит значение,
     * стёрт последний текст ядра — уходит вся лексема (AWP-11.5). No-op без
     * формы, без значения и без пустых текстов. Эффектов нет.
     */
    fun ScanCandidatesState.dropEmptyTexts(ref: ValueRefUi): ScanCandidatesResult {
        val form = form ?: return noOp("dropEmptyTexts: no form")
        return lemmaStep("dropEmptyTexts", ref.lemma, *ref.attrs()) { card ->
            listOf(card).droppingEmptyTexts(ref, form)?.single()
        }
    }

    /** Текст ответа обратно (AWP-16.6). No-op, если значения нет. Эффектов нет. */
    fun ScanCandidatesState.resetText(ref: ValueRefUi): ScanCandidatesResult =
        lemmaStep("resetText", ref.lemma, *ref.attrs()) { card -> listOf(card).withTextReset(ref)?.single() }

    /**
     * Опция «Выбора» у одной лексемы-кандидата (AWP-17): у компонента с одним
     * значением замена, у «несколько» переключатель, null — «без значения».
     * Активность зависимых значений — следующий атом цепочки. No-op без формы и
     * без значения. Эффектов нет.
     */
    fun ScanCandidatesState.setOption(
        ref: ComponentRefUi,
        option: Long?,
    ): ScanCandidatesResult {
        val form = form ?: return noOp("setOption: no form")
        val attrs = arrayOf<Pair<String, Any?>>(
            "key" to ref.key,
            "componentId" to ref.componentId,
            "option" to option,
        )
        return lemmaStep("setOption", ref.lemma, *attrs) { card -> listOf(card).withOption(ref, option, form)?.single() }
    }

    /** «Куда класть» карточки (AWP-18). No-op, если карточки нет. Эффектов нет. */
    fun ScanCandidatesState.setCardTarget(
        lemma: String,
        target: CardTargetUi,
    ): ScanCandidatesResult = lemmaStep("setCardTarget", lemma, "target" to target) { card ->
        listOf(card).withCardTarget(lemma, target)?.single()
    }

    /** Своя группа карточки (AWP-08). No-op, если карточки нет. Эффектов нет. */
    fun ScanCandidatesState.setCardGroup(
        lemma: String,
        group: CardOwnGroupUi,
    ): ScanCandidatesResult = lemmaStep("setCardGroup", lemma, "group" to group) { card ->
        listOf(card).withCardGroup(lemma, group)?.single()
    }

    /** Активность значений у карточек леммы (AWP-17.6). No-op без формы и без карточки леммы. Эффектов нет. */
    fun ScanCandidatesState.refreshLemmaValues(lemma: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshLemmaValues: no form")
        return lemmaCardsStep("refreshLemmaValues", lemma) { card -> card.copy(lemma = card.lemma.withActiveValues(form)) }
    }

    /** Сохранённые лексемы выбранной леммы у карточек леммы (AWP-18.8, AWP-46). No-op без формы и без карточки леммы. */
    fun ScanCandidatesState.refreshLemmaSaved(lemma: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshLemmaSaved: no form")
        return lemmaCardsStep("refreshLemmaSaved", lemma) { card -> card.copy(saved = form.savedLexemesOf(card.lemma.target)) }
    }

    /** «Уже в словаре», «не попадёт в ядро» и «Добавить» заново у карточек леммы. No-op без формы и без карточки леммы. */
    fun ScanCandidatesState.refreshLemmaMarks(lemma: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshLemmaMarks: no form")
        return lemmaCardsStep("refreshLemmaMarks", lemma) { card -> card.withMarks(form) }
    }

    /**
     * Сохранённые, счёт лемм словаря, активность и пометки всех карточек заново:
     * после сброса леммы словаря или компонента. No-op без формы.
     */
    fun ScanCandidatesState.refreshAllCards(): ScanCandidatesResult {
        val form = form ?: return noOp("refreshAllCards: no form")
        logStep("refreshAllCards", "cards" to lists.allLemmaCards().size)
        val next = lists.updateCards { card ->
            val lemma = card.lemma
                .copy(existingCount = form.existingOf(card.lemma.lemma).size)
                .withActiveValues(form)
            card.copy(saved = form.savedLexemesOf(lemma.target), lemma = lemma).withMarks(form)
        }
        return copy(lists = next).begin()
    }

    // ===== Общая группа и листы =====

    /**
     * Сменить общую группу (AWP-08): группа словаря или «Все» (null). Не
     * запоминается (AWP-08.7). Эффектов нет.
     */
    fun ScanCandidatesState.setCommonGroup(groupId: Long?): ScanCandidatesResult {
        logStep("setCommonGroup", "groupId" to groupId)
        return copy(commonGroupId = groupId).begin()
    }

    /**
     * Открыть лист, один за раз. У правки текста черновик — текущий текст
     * значения из карточек по адресу, «сбросить» есть, если текст отличается от
     * текста ответа (AWP-16.6, AWP-16.8). No-op, если значения под правку нет.
     * Эффектов нет.
     */
    fun ScanCandidatesState.openSheet(sheet: CandidatesSheetUi): ScanCandidatesResult {
        val opened = if (sheet is CandidatesSheetUi.TextEdit) {
            val text = lists.allLemmaCards().textOf(sheet.ref)
                ?: return noOp("openSheet: no text ${sheet.ref}")
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
        val original = lists.allLemmaCards().textOf(edit.ref)?.original
        logStep("updateSheetDraft", "length" to text.length)
        val next = edit.copy(draft = text, canReset = original != null && text.trim() != original)
        return copy(sheet = next).begin()
    }

    // ===== Запись слова и сбои =====

    /**
     * Запись слова (Ш9, AWP-20, AWP-03): пакет одного слова из карточки — лемма
     * новая или выбранная, лексемы без «уже в словаре», группа своя или общая;
     * «идёт запись» у слова и у экрана, прежний сбой снят; эффект записи. No-op
     * без формы, без карточки и без лексем на запись.
     */
    fun ScanCandidatesState.startAddingWord(text: String): ScanCandidatesResult {
        val form = form ?: return noOp("startAddingWord: no form")
        val card = lists.wordOf(text)?.card ?: return noOp("startAddingWord: no card $text")
        val batch = wordsBatchOf(listOf(card.lemma), form, commonGroupId)
        val lexemes = batch.words.sumOf { it.lexemes.size }
        if (lexemes == 0) return noOp("startAddingWord: nothing to write $text")
        logStep("startAddingWord", "word" to text, "lexemes" to lexemes)
        val next = copy(
            isAdding = true,
            lists = lists.updateWord(text) { it.copy(card = card.copy(isAdding = true, addFailure = null)) },
        )
        return next to setOf(DatasourceEffect.AddWord(text, batch))
    }

    /**
     * Записано (Ш9 → Ш5, Д118): строки слова прочь из списков, как будто
     * переехало в словарь; «идёт запись» экрана снят, когда других записей
     * нет (AWP-47). No-op без слова. Эффектов нет.
     */
    fun ScanCandidatesState.removeAddedWord(text: String): ScanCandidatesResult {
        lists.wordOf(text) ?: return noOp("removeAddedWord: no word $text")
        logStep("removeAddedWord", "word" to text)
        val rest = lists.removeWord(text)
        return copy(lists = rest, isAdding = rest.anyAdding()).begin()
    }

    /**
     * Запись не удалась (AWP-21): «не добавилось» с причиной и «Повторить» под
     * словом, кнопки слова снова активны; «идёт запись» экрана снят, когда
     * других записей нет. Сбросы недействительного выбора — отдельные атомы
     * цепочки. No-op без карточки. Эффектов нет.
     */
    fun ScanCandidatesState.showAddFailure(
        text: String,
        failure: AddFailureUi,
    ): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("showAddFailure: no card $text")
        logStep("showAddFailure", "word" to text, "failure" to failure)
        val rest = lists.updateWord(text) { it.copy(card = card.copy(isAdding = false, addFailure = failure)) }
        return copy(lists = rest, isAdding = rest.anyAdding()).begin()
    }

    /** Снять «не добавилось» перед повтором (AWP-26). No-op без карточки и без сбоя. Эффектов нет. */
    fun ScanCandidatesState.clearAddFailure(text: String): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("clearAddFailure: no card $text")
        card.addFailure ?: return noOp("clearAddFailure: no failure $text")
        logStep("clearAddFailure", "word" to text)
        return copy(lists = lists.updateWord(text) { it.copy(card = card.copy(addFailure = null)) }).begin()
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
            lists = lists.updateCards { card -> card.copy(lemma = listOf(card.lemma).withoutGroup(groupId).single()) },
            form = form?.withoutDeadGroup(groupId),
        ).begin()
    }

    /**
     * Удалённая лемма словаря (AWP-21.4): «куда класть» у карточек с ней → «новое
     * слово», из формы она уходит. Сохранённые и пометки заново — следующий
     * атом цепочки. Эффектов нет.
     * @param wordId id удалённой леммы.
     */
    fun ScanCandidatesState.resetDeadTarget(wordId: Long): ScanCandidatesResult {
        logStep("resetDeadTarget", "wordId" to wordId)
        return copy(
            lists = lists.updateCards { card -> card.copy(lemma = listOf(card.lemma).withoutTarget(wordId).single()) },
            form = form?.withoutDeadLemma(wordId),
        ).begin()
    }

    /**
     * Выключенный компонент (AWP-21.6): из формы уходит он сам, из лексем всех
     * карточек — его значения. Активность и пометки пересчитывает следующий
     * атом цепочки. No-op без формы. Эффектов нет.
     * @param typeId id компонента.
     */
    fun ScanCandidatesState.dropComponent(typeId: Long): ScanCandidatesResult {
        val form = form ?: return noOp("dropComponent: no form")
        logStep("dropComponent", "typeId" to typeId)
        return copy(
            form = form.copy(components = form.components.filterNot { it.id == typeId }),
            lists = lists.updateCards { card -> card.copy(lemma = listOf(card.lemma).withoutComponent(typeId).single()) },
        ).begin()
    }

    // ===== Закрытие =====

    /** Вопрос «Закрыть без добавления?» с числом слов, чьи лексемы-кандидаты пришли или идут и не добавлены (AWP-51). Эффектов нет. */
    fun ScanCandidatesState.askCloseConfirm(): ScanCandidatesResult {
        val pending = lists.countPending()
        logStep("askCloseConfirm", "pending" to pending)
        return copy(isConfirmingClose = true, pendingWords = pending).begin()
    }

    /** Убрать вопрос о закрытии («Остаться», AWP-51.3). Эффектов нет. */
    fun ScanCandidatesState.closeCloseConfirm(): ScanCandidatesResult {
        logStep("closeCloseConfirm")
        return copy(isConfirmingClose = false, pendingWords = 0).begin()
    }

    /** Назад на вкладку без результата (AWP-60): эффект навигации, стейт не меняется. */
    fun ScanCandidatesState.close(): ScanCandidatesResult {
        logStep("close")
        return this to setOf(NavigationEffect.Back)
    }

    // ===== Внутреннее: чистые помощники, не шаги, в лог не пишутся =====

    /** Карточка слова с текстом фазы: лемма и сохранённые как были. No-op без слова и без карточки. */
    private fun ScanCandidatesState.showCardMessage(
        step: String,
        text: String,
        phase: CardPhase,
        @StringRes messageRes: Int,
        hasRetryButton: Boolean,
    ): ScanCandidatesResult {
        val prior = lists.wordOf(text)?.card ?: return noOp("$step: no card $text")
        logStep(step, "word" to text, "res" to messageRes)
        val card = WordCardUi(
            phase = phase,
            lemma = prior.lemma,
            saved = prior.saved,
            errorRes = messageRes,
            hasRetryButton = hasRetryButton,
        )
        return copy(lists = lists.updateWord(text) { it.copy(card = card) }).begin()
    }

    /** Шаг над карточками леммы у всех слов: null от перехода у всех — no-op с именем шага. */
    private fun ScanCandidatesState.lemmaStep(
        step: String,
        lemma: String,
        vararg attrs: Pair<String, Any?>,
        change: (LemmaCardUiItem) -> LemmaCardUiItem?,
    ): ScanCandidatesResult {
        val next = lists.updateLemmaCards(lemma, change) ?: return noOp("$step: nothing to change")
        logStep(step, "lemma" to lemma, *attrs)
        return copy(lists = next).begin()
    }

    /** Пересчёт карточек слов с этой леммой: без таких карточек — no-op. */
    private fun ScanCandidatesState.lemmaCardsStep(
        step: String,
        lemma: String,
        change: (WordCardUi) -> WordCardUi,
    ): ScanCandidatesResult {
        if (lists.allLemmaCards().none { it.lemma == lemma }) return noOp("$step: no card $lemma")
        logStep(step, "lemma" to lemma)
        val next = lists.updateCards { card -> if (card.lemma.lemma == lemma) change(card) else card }
        return copy(lists = next).begin()
    }

    /** Пометки «уже в словаре» по ядрам выбранной леммы, «не попадёт в ядро» и «Добавить» карточки. */
    private fun WordCardUi.withMarks(form: DictionaryFormUi): WordCardUi {
        val marked = listOf(lemma)
            .withExistsMarks(form.lexemeKeysOf(lemma.target), form)
            .withCoreMarks(form)
            .single()
        val canAdd = marked.lexemes.any { !it.exists } && marked.lexemes.none { it.lacksCore }
        return copy(lemma = marked, canAdd = canAdd)
    }

    private fun LemmaCardUiItem.withActiveValues(form: DictionaryFormUi): LemmaCardUiItem =
        listOf(this).withActiveValues(form).single()

    private fun LemmaCardUiItem.activeValueCount(): Int = lexemes
        .flatMap { it.values }
        .count { it.isActive }

    private fun ValueRefUi.attrs(): Array<Pair<String, Any?>> = arrayOf(
        "key" to key,
        "componentId" to componentId,
        "index" to index,
    )

    private fun DictionaryFormUi.withoutDeadGroup(groupId: Long): DictionaryFormUi =
        copy(groups = groups.filterNot { it.id == groupId })

    private fun DictionaryFormUi.withoutDeadLemma(wordId: Long): DictionaryFormUi =
        copy(existingLemmas = existingLemmas.filterNot { it.wordId == wordId })
}
