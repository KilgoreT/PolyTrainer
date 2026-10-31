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
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.componentMatchLinesOf
import me.apomazkin.candidates.deletingLexeme
import me.apomazkin.candidates.deletingValue
import me.apomazkin.candidates.droppingEmptyTexts
import me.apomazkin.candidates.emptyLemmaCard
import me.apomazkin.candidates.existingOf
import me.apomazkin.candidates.lemmaCardOf
import me.apomazkin.candidates.lemmaWordKeys
import me.apomazkin.candidates.lexemeBatchOf
import me.apomazkin.candidates.remarksOf
import me.apomazkin.candidates.savedWordsOf
import me.apomazkin.candidates.textOf
import me.apomazkin.candidates.updateLexeme
import me.apomazkin.candidates.withActiveValues
import me.apomazkin.candidates.withCoreMarks
import me.apomazkin.candidates.withExistsMarks
import me.apomazkin.candidates.withLemmasOf
import me.apomazkin.candidates.withLexemeGroup
import me.apomazkin.candidates.withLexemeTarget
import me.apomazkin.candidates.withNewWordRetargeted
import me.apomazkin.candidates.withOption
import me.apomazkin.candidates.withTextEdit
import me.apomazkin.candidates.withTextReset
import me.apomazkin.candidates.withoutComponent
import me.apomazkin.candidates.withoutGroup
import me.apomazkin.candidates.withoutTarget
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
 * карточке слова; атом только адресует карточку — словом, которое и есть её
 * лемма, — или плашку словом и ключом лексемы-кандидата, и пишет лог.
 * Редьюсер и тест атомов наследуют этот класс.
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
     * регистра, краёв и формы Unicode (AWP-39); после перечитки лемм — заново
     * (AWP-63.5). No-op без формы. Эффектов нет.
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

    // ===== Под словом: плашки словаря, запрос, ответ =====

    /**
     * Карточка слова «в словаре» без сервера (Ш6 А, AWP-46): плашки словаря по
     * тексту слова, лемма без лексем; раскрыта. No-op без формы и без слова.
     * Эффектов нет.
     */
    fun ScanCandidatesState.openSavedCard(text: String): ScanCandidatesResult {
        val form = form ?: return noOp("openSavedCard: no form")
        lists.wordOf(text) ?: return noOp("openSavedCard: no word $text")
        val saved = form.savedWordsOf(text)
        logStep("openSavedCard", "word" to text, "saved" to saved.size)
        val card = WordCardUi(
            phase = CardPhase.SAVED,
            lemma = emptyLemmaCard(text, form),
            saved = saved,
        )
        return copy(lists = lists.updateWord(text) { it.copy(card = card, isExpanded = true) }).begin()
    }

    /**
     * Карточка «загрузка» раскрыта (Ш6 Б, Ш7, AWP-57): прежняя ошибка снята,
     * лемма и плашки словаря как были или пустая лемма у нового слова; эффект
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
     * Ответ в карточку (Ш8, AWP-43, AWP-49): лексемы-кандидаты первой леммы
     * ответа по сопоставлению в лемму с текстом слова, первая «в тексте», у
     * каждой «Куда» «новое слово» и группа «как у всех»; плашки словаря по
     * тексту слова; «Замечания» и «Сопоставление» по ответу (AWP-29, AWP-30,
     * AWP-31). Активность и пометки — следующие атомы цепочки. Слова уже нет —
     * ничего (AWP-48.4). No-op без формы. Эффектов нет.
     */
    fun ScanCandidatesState.applyLexemes(found: Msg.LexemesFound): ScanCandidatesResult {
        val form = form ?: return noOp("applyLexemes: no form")
        val word = lists.wordOf(found.text) ?: return noOp("applyLexemes: no word ${found.text}")
        val prior = word.card?.lemma ?: emptyLemmaCard(found.text, form)
        val lemma = lemmaCardOf(found.word, prior, form)
        logStep(
            "applyLexemes",
            "word" to found.text,
            "lemma" to found.word.lemmas.firstOrNull()?.text,
            "lexemes" to lemma.lexemes.size,
        )
        val card = WordCardUi(
            phase = CardPhase.CANDIDATES,
            lemma = lemma,
            saved = form.savedWordsOf(found.text),
            remarks = remarksOf(listOf(found.word), found.recommendations, form),
            componentMatchLines = componentMatchLinesOf(listOf(found.word), form),
        )
        return copy(lists = lists.updateWord(found.text) { it.copy(card = card) }).begin()
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
     * Пометки плашек слова: «уже в словаре» по ключам ядер всех слов словаря с
     * текстом слова (AWP-50, AWP-50.14), «не попадёт в ядро» (AWP-11). No-op
     * без формы и без карточки. Эффектов нет.
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
        )
        return copy(lists = lists.updateWord(text) { it.copy(card = marked) }).begin()
    }

    /** Карточка «не найдено» с текстом: плашек нейронки нет, «Убрать из списка» есть (AWP-55). No-op без карточки. */
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

    /** Всё под словом свёрнуто или раскрыто таким, каким было (AWP-42). No-op без слова и без карточки. Эффектов нет. */
    fun ScanCandidatesState.toggleCard(text: String): ScanCandidatesResult {
        val word = lists.wordOf(text) ?: return noOp("toggleCard: no word $text")
        word.card ?: return noOp("toggleCard: no card $text")
        logStep("toggleCard", "word" to text, "expanded" to !word.isExpanded)
        return copy(lists = lists.updateWord(text) { it.copy(isExpanded = !it.isExpanded) }).begin()
    }

    /**
     * «Убрать из списка»: строки слова прочь из всех списков без записи;
     * ответ, который придёт позже, некуда класть (AWP-48). No-op без слова.
     * Эффектов нет.
     */
    fun ScanCandidatesState.removeWord(text: String): ScanCandidatesResult {
        lists.wordOf(text) ?: return noOp("removeWord: no word $text")
        logStep("removeWord", "word" to text)
        return copy(lists = lists.removeWord(text)).begin()
    }

    /** Раздел «Сопоставление» под плашками слова раскрыт или свёрнут (AWP-31.4). No-op без карточки. Эффектов нет. */
    fun ScanCandidatesState.toggleCardMatches(text: String): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("toggleCardMatches: no card $text")
        val expanded = !card.isComponentMatchesExpanded
        logStep("toggleCardMatches", "word" to text, "expanded" to expanded)
        return copy(
            lists = lists.updateWord(text) { it.copy(card = card.copy(isComponentMatchesExpanded = expanded)) },
        ).begin()
    }

    // ===== Плашка нейронки: «Не нужно», уход слова, «Куда», группа =====

    /** «Не нужно»: лексема-кандидат со всеми значениями прочь из леммы слова (AWP-64). No-op, если её нет. Эффектов нет. */
    fun ScanCandidatesState.dismissLexeme(
        text: String,
        key: String,
    ): ScanCandidatesResult = lemmaStep("dismissLexeme", text, "key" to key) { card ->
        listOf(card).deletingLexeme(text, key)?.single()
    }

    /**
     * У слова ответ и ни одной плашки нейронки — последняя записана, убрана
     * или стёрта до пустого ядра: строки слова прочь из списков вместе с
     * плашками словаря (AWP-62). No-op без ответа и пока плашки есть.
     * Эффектов нет.
     */
    fun ScanCandidatesState.removeEmptyWord(text: String): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("removeEmptyWord: no card $text")
        if (card.phase != CardPhase.CANDIDATES) return noOp("removeEmptyWord: no answer $text")
        if (card.lemma.lexemes.isNotEmpty()) return noOp("removeEmptyWord: lexemes left $text")
        logStep("removeEmptyWord", "word" to text)
        return copy(lists = lists.removeWord(text)).begin()
    }

    /** «Куда» плашки (AWP-18). No-op, если плашки нет. Эффектов нет. */
    fun ScanCandidatesState.setLexemeTarget(
        text: String,
        key: String,
        target: CardTargetUi,
    ): ScanCandidatesResult = lemmaStep("setLexemeTarget", text, "key" to key, "target" to target) { card ->
        listOf(card).withLexemeTarget(text, key, target)?.single()
    }

    /** Своя группа плашки при «новое слово» (AWP-08). No-op, если плашки нет. Эффектов нет. */
    fun ScanCandidatesState.setLexemeGroup(
        text: String,
        key: String,
        group: CardOwnGroupUi,
    ): ScanCandidatesResult = lemmaStep("setLexemeGroup", text, "key" to key, "group" to group) { card ->
        listOf(card).withLexemeGroup(text, key, group)?.single()
    }

    // ===== Правки: адрес — лемма карточки, то есть текст слова =====

    /**
     * Одно значение компонента прочь (AWP-15.2, 15.7); ушёл последний текст
     * ядра — уходит вся плашка (AWP-11.5). No-op без формы и без значения.
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
     * стёрт последний текст ядра — уходит вся плашка (AWP-11.5). No-op без
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

    /** Активность значений у карточек леммы (AWP-17.6). No-op без формы и без карточки леммы. Эффектов нет. */
    fun ScanCandidatesState.refreshLemmaValues(lemma: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshLemmaValues: no form")
        return lemmaCardsStep("refreshLemmaValues", lemma) { card -> card.copy(lemma = card.lemma.withActiveValues(form)) }
    }

    /** «Уже в словаре» и «не попадёт в ядро» заново у карточек леммы. No-op без формы и без карточки леммы. */
    fun ScanCandidatesState.refreshLemmaMarks(lemma: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshLemmaMarks: no form")
        return lemmaCardsStep("refreshLemmaMarks", lemma) { card -> card.withMarks(form) }
    }

    /** Плашки словаря слова по форме заново (AWP-46, AWP-63). No-op без формы и без карточки. Эффектов нет. */
    fun ScanCandidatesState.refreshCardSaved(text: String): ScanCandidatesResult {
        val form = form ?: return noOp("refreshCardSaved: no form")
        val card = lists.wordOf(text)?.card ?: return noOp("refreshCardSaved: no card $text")
        val saved = form.savedWordsOf(text)
        logStep("refreshCardSaved", "word" to text, "saved" to saved.size)
        return copy(lists = lists.updateWord(text) { it.copy(card = card.copy(saved = saved)) }).begin()
    }

    /**
     * Плашки словаря, счёт лемм словаря, активность и пометки всех карточек
     * заново: после сброса леммы словаря или компонента. No-op без формы.
     */
    fun ScanCandidatesState.refreshAllCards(): ScanCandidatesResult {
        val form = form ?: return noOp("refreshAllCards: no form")
        logStep("refreshAllCards", "cards" to lists.allLemmaCards().size)
        val next = lists.updateCards { card ->
            val lemma = card.lemma
                .copy(existingCount = form.existingOf(card.lemma.lemma).size)
                .withActiveValues(form)
            card.copy(saved = form.savedWordsOf(card.lemma.lemma), lemma = lemma).withMarks(form)
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

    // ===== Запись плашки, перечитка и сбои =====

    /**
     * Запись плашки (Ш9, AWP-20, AWP-02, AWP-03): пакет одной лексемы — в
     * новое слово с группой, своей или общей, или в выбранное по «Куда» без
     * группы; эффект по типу «Куда» — «Создать» или «Перенести»; «идёт
     * запись» у плашки, карточки и экрана, прежний сбой плашки снят. No-op без
     * формы, без плашки и у плашки без ядра или «уже в словаре».
     */
    fun ScanCandidatesState.startAddingLexeme(
        text: String,
        key: String,
    ): ScanCandidatesResult {
        val form = form ?: return noOp("startAddingLexeme: no form")
        val card = lists.wordOf(text)?.card ?: return noOp("startAddingLexeme: no card $text")
        val lexeme = card.lexemeOf(key) ?: return noOp("startAddingLexeme: no lexeme $key")
        if (!lexeme.canWrite) return noOp("startAddingLexeme: not writable $key")
        val batch = lexemeBatchOf(card.lemma, key, form, commonGroupId)
            ?: return noOp("startAddingLexeme: no batch $key")
        logStep("startAddingLexeme", "word" to text, "key" to key, "target" to lexeme.target)
        val adding = card.copy(
            isAdding = true,
            lemma = card.lemma.withLexeme(key) { it.copy(isAdding = true, addFailure = null) },
        )
        val next = copy(isAdding = true, lists = lists.updateWord(text) { it.copy(card = adding) })
        val effect = when (lexeme.target) {
            CardTargetUi.NewWord -> DatasourceEffect.CreateWord(text, key, batch)
            is CardTargetUi.Existing -> DatasourceEffect.TransferLexeme(text, key, batch)
        }
        return next to setOf(effect)
    }

    /**
     * Записано: плашка прочь, «идёт запись» карточки снят, у экрана — когда
     * других записей нет (AWP-20, AWP-62). No-op без плашки. Эффектов нет.
     */
    fun ScanCandidatesState.finishAdding(
        text: String,
        key: String,
    ): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("finishAdding: no card $text")
        card.lexemeOf(key) ?: return noOp("finishAdding: no lexeme $key")
        logStep("finishAdding", "word" to text, "key" to key)
        val lemma = listOf(card.lemma).deletingLexeme(card.lemma.lemma, key)?.single() ?: card.lemma
        val rest = lists.updateWord(text) { it.copy(card = card.copy(isAdding = false, lemma = lemma)) }
        return copy(lists = rest, isAdding = rest.anyAdding()).begin()
    }

    /**
     * После «Создать»: у плашек слова с «Куда: новое слово» — созданное слово,
     * кнопка станет «Перенести» (AWP-63.1, 63.3); явный выбор не трогается.
     * No-op без таких плашек. Эффектов нет.
     */
    fun ScanCandidatesState.retargetNewWordLexemes(
        text: String,
        wordId: Long,
    ): ScanCandidatesResult = lemmaStep("retargetNewWordLexemes", text, "wordId" to wordId) { card ->
        listOf(card).withNewWordRetargeted(text, wordId)?.single()
    }

    /**
     * Эффект перечитки лемм словаря с текстом слова после записи (AWP-63).
     * No-op без словаря и без слова в списках: оно уже ушло.
     */
    fun ScanCandidatesState.startLemmasReload(text: String): ScanCandidatesResult {
        val dictionaryId = dictionaryId ?: return noOp("startLemmasReload: no dictionary")
        lists.wordOf(text) ?: return noOp("startLemmasReload: no word $text")
        logStep("startLemmasReload", "word" to text)
        return this to setOf(DatasourceEffect.ReloadLemmas(text, dictionaryId))
    }

    /**
     * Леммы словаря с текстом слова в форме заменены присланными, плашки
     * словаря и счёт лемм у карточки заново (AWP-63). Пометки «в словаре» и
     * «уже в словаре» — следующие атомы цепочки. No-op без формы. Эффектов
     * нет.
     */
    fun ScanCandidatesState.applyReloadedLemmas(
        text: String,
        existing: List<ExistingLemmaUi>,
    ): ScanCandidatesResult {
        val form = form?.withLemmasOf(text, existing) ?: return noOp("applyReloadedLemmas: no form")
        logStep("applyReloadedLemmas", "word" to text, "lemmas" to existing.size)
        val next = lists.updateWord(text) { item ->
            val card = item.card ?: return@updateWord item
            val lemma = card.lemma.copy(existingCount = form.existingOf(text).size)
            item.copy(card = card.copy(saved = form.savedWordsOf(text), lemma = lemma))
        }
        return copy(form = form, lists = next).begin()
    }

    /**
     * Запись не удалась (AWP-21): «не добавилось» с причиной и «Повторить» под
     * плашкой, кнопки плашек слова снова активны; «идёт запись» экрана снят,
     * когда других записей нет. Сбросы недействительного выбора — отдельные
     * атомы цепочки. No-op без плашки. Эффектов нет.
     */
    fun ScanCandidatesState.showAddFailure(
        text: String,
        key: String,
        failure: AddFailureUi,
    ): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("showAddFailure: no card $text")
        card.lexemeOf(key) ?: return noOp("showAddFailure: no lexeme $key")
        logStep("showAddFailure", "word" to text, "key" to key, "failure" to failure)
        val lemma = card.lemma.withLexeme(key) { it.copy(isAdding = false, addFailure = failure) }
        val rest = lists.updateWord(text) { it.copy(card = card.copy(isAdding = false, lemma = lemma)) }
        return copy(lists = rest, isAdding = rest.anyAdding()).begin()
    }

    /** Снять «не добавилось» у плашки перед повтором (AWP-26). No-op без плашки и без сбоя. Эффектов нет. */
    fun ScanCandidatesState.clearAddFailure(
        text: String,
        key: String,
    ): ScanCandidatesResult {
        val card = lists.wordOf(text)?.card ?: return noOp("clearAddFailure: no card $text")
        val lexeme = card.lexemeOf(key) ?: return noOp("clearAddFailure: no lexeme $key")
        lexeme.addFailure ?: return noOp("clearAddFailure: no failure $key")
        logStep("clearAddFailure", "word" to text, "key" to key)
        val lemma = card.lemma.withLexeme(key) { it.copy(addFailure = null) }
        return copy(lists = lists.updateWord(text) { it.copy(card = card.copy(lemma = lemma)) }).begin()
    }

    /**
     * Удалённая группа (AWP-21.3, 21.5): общая → «Все», своя у плашек → «как у
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
     * Удалённая лемма словаря (AWP-21.4): «Куда» у плашек с ней → «новое
     * слово», из формы она уходит. Плашки словаря и пометки заново — следующий
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

    /** Вопрос «Закрыть без добавления?» с числом слов, у которых плашки грузятся или остались неразобранные (AWP-51). Эффектов нет. */
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

    /** Карточка слова с текстом фазы: лемма и плашки словаря как были. No-op без слова и без карточки. */
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

    /** Пометки «уже в словаре» по ядрам всех слов словаря с текстом слова и «не попадёт в ядро» у плашек карточки. */
    private fun WordCardUi.withMarks(form: DictionaryFormUi): WordCardUi {
        val marked = listOf(lemma)
            .withExistsMarks(form.lemmaWordKeys(lemma.lemma), form)
            .withCoreMarks(form)
            .single()
        return copy(lemma = marked)
    }

    /** Плашка карточки по ключу лексемы-кандидата; null — её нет. */
    private fun WordCardUi.lexemeOf(key: String): CardLexemeUiItem? = lemma.lexemes.firstOrNull { it.key == key }

    /** Лемма с изменённой лексемой-кандидатом по ключу; остальные как были. */
    private fun LemmaCardUiItem.withLexeme(
        key: String,
        change: (CardLexemeUiItem) -> CardLexemeUiItem,
    ): LemmaCardUiItem = listOf(this).updateLexeme(lemma, key, change).single()

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
