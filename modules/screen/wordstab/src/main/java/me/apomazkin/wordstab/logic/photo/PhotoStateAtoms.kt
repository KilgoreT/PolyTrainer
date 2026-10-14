package me.apomazkin.wordstab.logic.photo

import me.apomazkin.candidates.PhotoCardLexeme
import me.apomazkin.candidates.cardOf
import me.apomazkin.candidates.lexemeOf
import me.apomazkin.candidates.originalOf
import me.apomazkin.candidates.updateCard
import me.apomazkin.candidates.updateLexeme
import me.apomazkin.candidates.withText
import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoCard
import me.apomazkin.candidates.PhotoCardTarget
import me.apomazkin.candidates.PhotoDataKind
import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoFormError
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.candidates.PhotoOwnGroup
import me.apomazkin.candidates.PhotoSheet
import me.apomazkin.candidates.PhotoTarget
import me.apomazkin.candidates.PhotoTextField
import me.apomazkin.candidates.PhotoWord
import me.apomazkin.candidates.buildPhotoCards
import me.apomazkin.candidates.choose
import me.apomazkin.candidates.resolveMapping
import me.apomazkin.candidates.revalidate
import me.apomazkin.candidates.summaryOf
import me.apomazkin.candidates.withCoreMarks
import me.apomazkin.candidates.withExistsMarks
import androidx.annotation.StringRes
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ReducerLogging
import me.apomazkin.wordstab.LogTags
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.PhotoPhase
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.WordsTabState

internal typealias WordsResult = ReducerResult<WordsTabState, Effect>

/**
 * ###### Атомарные state-экстеншны диалога «слова с фото» и окна
 * добавления (конвенция §Reducer-конвенция): один экстеншн = ОДИН
 * технический шаг, каждый возвращает [ReducerResult] и логируется
 * [logStep]; атом без своих данных — [noOp] с причиной. Оркестраторов
 * нет: guard'ы и цепочки — в ветках [me.apomazkin.wordstab.logic.WordsTabReducer].
 * Reducer и тест атомов наследуют этот класс. ######
 */
abstract class PhotoStateAtoms(
    logger: LexemeLogger,
) : ReducerLogging(logger) {
    /** Фичевый тег вкладки: шаги, сообщения и логи handler'а — под одним `###VOCAB###`. */
    override val logTag: String = LogTags.VOCAB

    /** ###### ЗАПУСК: ВЫБОР ИСТОЧНИКА, РАСПОЗНАВАНИЕ, ЗАПРОС ###### */

    /**
     * Открыть диалог на шаге выбора источника с НОВЫМ номером запуска:
     * результат прежнего снимка на шаг выбора не попадёт (WFT-01).
     * Меняет [WordsTabState.photoWordsState] целиком. Эффектов нет.
     */
    fun WordsTabState.showPhotoSourceChooser(): WordsResult {
        val requestId = photoWordsState.requestId + 1
        logStep("showPhotoSourceChooser", "requestId" to requestId)
        return copy(photoWordsState = PhotoWordsState(isOpen = true, isChoosingSource = true, requestId = requestId)).begin()
    }

    /**
     * Вернуться к выбору источника без снимка — тот же запуск, тот же
     * номер (В1, WFT-02.2). Меняет [WordsTabState.photoWordsState] целиком.
     * Эффектов нет.
     */
    fun WordsTabState.returnToPhotoSourceChooser(): WordsResult {
        logStep("returnToPhotoSourceChooser", "requestId" to photoWordsState.requestId)
        return copy(
            photoWordsState = PhotoWordsState(isOpen = true, isChoosingSource = true, requestId = photoWordsState.requestId),
        ).begin()
    }

    /**
     * Начать распознавание снимка с НОВЫМ номером запуска: результаты
     * прежних распознаваний устарели (WFT-16.1). Эффект распознавания
     * добавляет ветка reducer'а (ей известен uri). Меняет
     * [WordsTabState.photoWordsState] целиком.
     */
    fun WordsTabState.startRecognizing(): WordsResult {
        val requestId = photoWordsState.requestId + 1
        logStep("startRecognizing", "requestId" to requestId)
        return copy(photoWordsState = PhotoWordsState(isOpen = true, phase = PhotoPhase.RECOGNIZING, requestId = requestId)).begin()
    }

    /**
     * Токены ушли на сервер — тот же запуск, подпись «Ищу слова в словаре»
     * (WFT-16.2). Эффект запроса добавляет ветка reducer'а (ей известны
     * токены). Меняет [WordsTabState.photoWordsState] целиком.
     */
    fun WordsTabState.startLooking(): WordsResult {
        logStep("startLooking", "requestId" to photoWordsState.requestId)
        return copy(
            photoWordsState = PhotoWordsState(isOpen = true, phase = PhotoPhase.LOOKING, requestId = photoWordsState.requestId),
        ).begin()
    }

    /**
     * Принять ответ сервера со словами и начать сборку окна (Ш1): слова и
     * словарь снимка — в state, индикатор — фаза FORM (AWP-24), эффект
     * загрузки формы рождается здесь — ему нужны словарь, тексты лемм и
     * номер запуска.
     * @param dictionaryId словарь снимка из исхода запроса (AWP-01).
     * @param words слова ответа в порядке сервера.
     */
    fun WordsTabState.startFormLoading(
        dictionaryId: Long,
        words: List<PhotoWord>,
    ): WordsResult {
        val lemmaTexts = words.flatMap { word -> word.lemmas.map { it.text } }.distinct()
        logStep("startFormLoading", "dictionaryId" to dictionaryId, "words" to words.size, "lemmas" to lemmaTexts.size)
        val next = copy(
            photoWordsState = PhotoWordsState(
                isOpen = true,
                phase = PhotoPhase.FORM,
                words = words,
                dictionaryId = dictionaryId,
                requestId = photoWordsState.requestId,
            ),
        )
        return next to setOf(
            DatasourceEffect.LoadPhotoForm(
                dictionaryId = dictionaryId,
                lemmaTexts = lemmaTexts,
                requestId = photoWordsState.requestId,
            ),
        )
    }

    /**
     * Показать «слов не найдено» без индикатора (В2, WFT-60): токенов нет
     * или ответ пуст. Меняет [WordsTabState.photoWordsState] целиком.
     * Эффектов нет.
     */
    fun WordsTabState.showNoPhotoWords(): WordsResult {
        logStep("showNoPhotoWords")
        return copy(
            photoWordsState = PhotoWordsState(isOpen = true, isEmptyResult = true, requestId = photoWordsState.requestId),
        ).begin()
    }

    /**
     * Показать ошибку запуска или отказ сервера текстом ресурсом, индикатор
     * закрыт (В3, В5). Меняет [WordsTabState.photoWordsState] целиком.
     * Эффектов нет.
     * @param errorRes строка ошибки.
     */
    fun WordsTabState.showPhotoError(@StringRes errorRes: Int): WordsResult {
        logStep("showPhotoError", "errorRes" to errorRes)
        return copy(
            photoWordsState = PhotoWordsState(isOpen = true, errorRes = errorRes, requestId = photoWordsState.requestId),
        ).begin()
    }

    /**
     * Закрыть диалог: всё сбрасывается, кроме номера запуска (WFT-01).
     * Зовётся на любом шаге, в том числе после записи (AWP-23) и по
     * «закрыть» в вопросе (AWP-25). Эффектов нет.
     */
    fun WordsTabState.hidePhotoWords(): WordsResult {
        logStep("hidePhotoWords", "requestId" to photoWordsState.requestId)
        return copy(photoWordsState = PhotoWordsState(requestId = photoWordsState.requestId)).begin()
    }

    /** ###### ОКНО: ФОРМА, КАРТОЧКИ, ПОМЕТКИ ###### */

    /**
     * Принять форму словаря (Ш1 → Ш2): индикатор гаснет, соответствия —
     * из запомненных с умолчаниями (AWP-06), карточки и «не добавить» —
     * из слов ответа (AWP-04, AWP-05, AWP-12), общая группа — «Все».
     * Пометки «есть», «ядро» и сводка — следующие атомы цепочки
     * ([refreshExistsMarks], [refreshCoreMarks], [refreshSummary]).
     * @param form форма словаря снимка.
     * @param remembered запомненные соответствия или null.
     * Эффектов нет.
     */
    fun WordsTabState.applyForm(
        form: PhotoDictionaryForm,
        remembered: PhotoMapping?,
    ): WordsResult {
        val build = buildPhotoCards(photoWordsState.words, form)
        logStep("applyForm", "components" to form.components.size, "groups" to form.groups.size, "cards" to build.cards.size, "notAdded" to build.notAdded.size)
        return copy(
            photoWordsState = photoWordsState.copy(
                phase = PhotoPhase.IDLE,
                form = form,
                formError = null,
                mapping = form.resolveMapping(remembered),
                commonGroupId = null,
                cards = build.cards,
                notAdded = build.notAdded,
                isNotAddedExpanded = build.cards.isEmpty(),
            ),
        ).begin()
    }

    /**
     * Форма не загрузилась (В1, AWP-24): индикатор гаснет, причина — в
     * [PhotoWordsState.formError]. Эффектов нет.
     * @param error словарь удалён или чтение упало.
     */
    fun WordsTabState.showFormError(error: PhotoFormError): WordsResult {
        logStep("showFormError", "error" to error)
        return copy(photoWordsState = photoWordsState.copy(phase = PhotoPhase.IDLE, formError = error)).begin()
    }

    /**
     * Пересчитать пометки «есть» у всех лексем-кандидатов (AWP-07) по
     * леммам словаря из формы. Зовётся после сборки карточек и после
     * правки текста. No-op без формы. Эффектов нет.
     */
    fun WordsTabState.refreshExistsMarks(): WordsResult {
        val form = photoWordsState.form ?: return noOp("refreshExistsMarks: no form")
        val cards = photoWordsState.cards.withExistsMarks(form)
        logStep("refreshExistsMarks", "exists" to cards.sumOf { card -> card.lexemes.count { it.exists } })
        return copy(photoWordsState = photoWordsState.copy(cards = cards)).begin()
    }

    /**
     * Пересчитать пометки «не попадёт в ядро» (AWP-11) при текущих
     * соответствиях. Зовётся после сборки карточек, смены соответствий и
     * правки текста. No-op без формы. Эффектов нет.
     */
    fun WordsTabState.refreshCoreMarks(): WordsResult {
        val form = photoWordsState.form ?: return noOp("refreshCoreMarks: no form")
        val cards = photoWordsState.cards.withCoreMarks(photoWordsState.mapping, form)
        logStep("refreshCoreMarks", "lacksCore" to cards.sumOf { card -> card.lexemes.count { it.lacksCore } })
        return copy(photoWordsState = photoWordsState.copy(cards = cards)).begin()
    }

    /**
     * Пересчитать сводку кнопки «добавить» (AWP-13) по карточкам и
     * соответствиям. Последний атом каждой цепочки действия. Эффектов нет.
     */
    fun WordsTabState.refreshSummary(): WordsResult {
        val summary = summaryOf(photoWordsState.cards, photoWordsState.mapping, photoWordsState.form)
        logStep("refreshSummary", "lexemes" to summary.lexemeCount, "words" to summary.wordCount, "existing" to summary.existingCount, "lacksCore" to summary.lacksCoreCount, "enabled" to summary.isEnabled)
        return copy(photoWordsState = photoWordsState.copy(summary = summary)).begin()
    }

    /** ###### ДЕЙСТВИЯ В ОКНЕ (Ш3) ###### */

    /**
     * Развернуть или свернуть карточку.
     * @param lemma текст леммы карточки.
     * No-op, если карточки нет. Эффектов нет.
     */
    fun WordsTabState.toggleCard(lemma: String): WordsResult {
        val card = cardOf(lemma) ?: return noOp("toggleCard: no card $lemma")
        logStep("toggleCard", "lemma" to lemma, "expanded" to !card.isExpanded)
        return updateCard(lemma) { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * Удалить карточку — лемму со всеми лексемами-кандидатами, безвозвратно
     * (AWP-15). Слова, из которых она пришла, в «не добавить» не попадают
     * (AWP-15.4). Сводку пересчитывает [refreshSummary].
     * @param lemma текст леммы карточки.
     * No-op, если карточки нет. Эффектов нет.
     */
    fun WordsTabState.deleteCard(lemma: String): WordsResult {
        cardOf(lemma) ?: return noOp("deleteCard: no card $lemma")
        logStep("deleteCard", "lemma" to lemma)
        return copy(photoWordsState = photoWordsState.copy(cards = photoWordsState.cards.filterNot { it.lemma == lemma })).begin()
    }

    /**
     * Удалить лексему-кандидата с её примерами (AWP-15.1); скрытые на её
     * место не поднимаются, «ещё N» не меняется (AWP-05.4).
     * @param lemma текст леммы карточки.
     * @param key ключ лексемы-кандидата.
     * No-op, если её нет. Эффектов нет.
     */
    fun WordsTabState.deleteLexeme(
        lemma: String,
        key: String,
    ): WordsResult {
        lexemeOf(lemma, key) ?: return noOp("deleteLexeme: no lexeme $lemma/$key")
        logStep("deleteLexeme", "lemma" to lemma, "key" to key)
        return updateCard(lemma) { card -> card.copy(lexemes = card.lexemes.filterNot { it.key == key }) }
    }

    /**
     * Удалить один пример лексемы-кандидата (AWP-15.2).
     * @param lemma текст леммы карточки.
     * @param key ключ лексемы-кандидата.
     * @param index номер примера в списке.
     * No-op, если примера нет. Эффектов нет.
     */
    fun WordsTabState.deleteExample(
        lemma: String,
        key: String,
        index: Int,
    ): WordsResult {
        val lexeme = lexemeOf(lemma, key) ?: return noOp("deleteExample: no lexeme $lemma/$key")
        if (index !in lexeme.examples.indices) return noOp("deleteExample: no example $index")
        logStep("deleteExample", "lemma" to lemma, "key" to key, "index" to index)
        return updateLexeme(lemma, key) { it.copy(examples = it.examples.filterIndexed { i, _ -> i != index }) }
    }

    /**
     * Принять правку текста лексемы-кандидата с обрезанными краями
     * (AWP-16): перевод, толкование или пример. Пустой пример убирает
     * следующий атом цепочки ([dropEmptyExamples]); пометки — дальше по
     * цепочке.
     * @param lemma текст леммы карточки.
     * @param key ключ лексемы-кандидата.
     * @param field какой текст.
     * @param text новый текст.
     * No-op, если лексемы или поля нет. Эффектов нет.
     */
    fun WordsTabState.applyTextEdit(
        lemma: String,
        key: String,
        field: PhotoTextField,
        text: String,
    ): WordsResult {
        val lexeme = lexemeOf(lemma, key) ?: return noOp("applyTextEdit: no lexeme $lemma/$key")
        val trimmed = text.trim()
        val edited = lexeme.withText(field, trimmed) ?: return noOp("applyTextEdit: no field $field")
        logStep("applyTextEdit", "lemma" to lemma, "key" to key, "field" to field, "length" to trimmed.length)
        return updateLexeme(lemma, key) { edited }
    }

    /**
     * Убрать примеры со стёртым текстом (AWP-16.5): пустой пример —
     * удалённый. Зовётся после [applyTextEdit].
     * @param lemma текст леммы карточки.
     * @param key ключ лексемы-кандидата.
     * No-op, если лексемы нет или пустых примеров нет. Эффектов нет.
     */
    fun WordsTabState.dropEmptyExamples(
        lemma: String,
        key: String,
    ): WordsResult {
        val lexeme = lexemeOf(lemma, key) ?: return noOp("dropEmptyExamples: no lexeme $lemma/$key")
        val kept = lexeme.examples.filter { it.text.isNotBlank() }
        if (kept.size == lexeme.examples.size) return noOp("dropEmptyExamples: none empty")
        logStep("dropEmptyExamples", "lemma" to lemma, "key" to key, "dropped" to lexeme.examples.size - kept.size)
        return updateLexeme(lemma, key) { it.copy(examples = kept) }
    }

    /**
     * Вернуть тексту лексемы-кандидата текст источника (AWP-16.6).
     * @param lemma текст леммы карточки.
     * @param key ключ лексемы-кандидата.
     * @param field какой текст.
     * No-op, если лексемы или поля нет. Эффектов нет.
     */
    fun WordsTabState.resetText(
        lemma: String,
        key: String,
        field: PhotoTextField,
    ): WordsResult {
        val lexeme = lexemeOf(lemma, key) ?: return noOp("resetText: no lexeme $lemma/$key")
        val original = lexeme.originalOf(field) ?: return noOp("resetText: no field $field")
        val reset = lexeme.withText(field, original) ?: return noOp("resetText: no field $field")
        logStep("resetText", "lemma" to lemma, "key" to key, "field" to field)
        return updateLexeme(lemma, key) { reset }
    }

    /**
     * Поставить часть речи лексемы-кандидата (AWP-17): опция «Части речи»
     * или «без части речи»; на остальные лексемы не действует.
     * @param lemma текст леммы карточки.
     * @param key ключ лексемы-кандидата.
     * @param optionId опция или null.
     * No-op, если лексемы нет. Эффектов нет.
     */
    fun WordsTabState.setPartOfSpeech(
        lemma: String,
        key: String,
        optionId: Long?,
    ): WordsResult {
        lexemeOf(lemma, key) ?: return noOp("setPartOfSpeech: no lexeme $lemma/$key")
        logStep("setPartOfSpeech", "lemma" to lemma, "key" to key, "optionId" to optionId)
        return updateLexeme(lemma, key) { it.copy(partOfSpeechOptionId = optionId) }
    }

    /**
     * Выбрать, куда класть лексемы карточки (AWP-18): существующая лемма
     * словаря или новая.
     * @param lemma текст леммы карточки.
     * @param target выбор.
     * No-op, если карточки нет. Эффектов нет.
     */
    fun WordsTabState.setCardTarget(
        lemma: String,
        target: PhotoCardTarget,
    ): WordsResult {
        cardOf(lemma) ?: return noOp("setCardTarget: no card $lemma")
        logStep("setCardTarget", "lemma" to lemma, "target" to target)
        return updateCard(lemma) { it.copy(target = target) }
    }

    /**
     * Поставить или снять свою группу карточки (AWP-08).
     * @param lemma текст леммы карточки.
     * @param group как у всех, «Все» или группа.
     * No-op, если карточки нет. Эффектов нет.
     */
    fun WordsTabState.setCardGroup(
        lemma: String,
        group: PhotoOwnGroup,
    ): WordsResult {
        cardOf(lemma) ?: return noOp("setCardGroup: no card $lemma")
        logStep("setCardGroup", "lemma" to lemma, "group" to group)
        return updateCard(lemma) { it.copy(ownGroup = group) }
    }

    /**
     * Сменить общую группу (AWP-08): группа словаря или «Все» (null). Не
     * запоминается (AWP-06.8). Эффектов нет.
     * @param groupId группа или null.
     */
    fun WordsTabState.setCommonGroup(groupId: Long?): WordsResult {
        logStep("setCommonGroup", "groupId" to groupId)
        return copy(photoWordsState = photoWordsState.copy(commonGroupId = groupId)).begin()
    }

    /**
     * Выбрать цель вида данных (AWP-09): компонент, занятый другим видом,
     * освобождается (AWP-28.1). Эффект запоминания рождается здесь — ему
     * нужны словарь и новые соответствия (AWP-06). Пометки «ядро» и сводку
     * пересчитывают следующие атомы цепочки.
     * @param kind вид данных.
     * @param target компонент или «не добавлять».
     * No-op без словаря снимка.
     */
    fun WordsTabState.setMappingTarget(
        kind: PhotoDataKind,
        target: PhotoTarget,
    ): WordsResult {
        val dictionaryId = photoWordsState.dictionaryId ?: return noOp("setMappingTarget: no dictionary")
        val mapping = photoWordsState.mapping.choose(kind, target)
        logStep("setMappingTarget", "kind" to kind, "target" to target)
        return copy(photoWordsState = photoWordsState.copy(mapping = mapping)) to setOf(
            DatasourceEffect.SavePhotoMapping(dictionaryId = dictionaryId, mapping = mapping),
        )
    }

    /** Раскрыть или свернуть блок соответствий (AWP-09.5). Эффектов нет. */
    fun WordsTabState.toggleMappingExpanded(): WordsResult {
        val expanded = !photoWordsState.isMappingExpanded
        logStep("toggleMappingExpanded", "expanded" to expanded)
        return copy(photoWordsState = photoWordsState.copy(isMappingExpanded = expanded)).begin()
    }

    /** Раскрыть или свернуть блок «не добавить» (AWP-12.2). Эффектов нет. */
    fun WordsTabState.toggleNotAddedExpanded(): WordsResult {
        val expanded = !photoWordsState.isNotAddedExpanded
        logStep("toggleNotAddedExpanded", "expanded" to expanded)
        return copy(photoWordsState = photoWordsState.copy(isNotAddedExpanded = expanded)).begin()
    }

    /**
     * Открыть лист выбора или правки (один за раз).
     * @param sheet какой лист.
     * Эффектов нет.
     */
    fun WordsTabState.openSheet(sheet: PhotoSheet): WordsResult {
        logStep("openSheet", "sheet" to sheet::class.simpleName)
        return copy(photoWordsState = photoWordsState.copy(sheet = sheet)).begin()
    }

    /** Закрыть открытый лист. No-op, если листа нет. Эффектов нет. */
    fun WordsTabState.closeSheet(): WordsResult {
        photoWordsState.sheet ?: return noOp("closeSheet: no sheet")
        logStep("closeSheet")
        return copy(photoWordsState = photoWordsState.copy(sheet = null)).begin()
    }

    /**
     * Принять ввод листа правки текста.
     * @param text полный текущий текст поля.
     * No-op, если открыт не лист правки. Эффектов нет.
     */
    fun WordsTabState.updateSheetDraft(text: String): WordsResult {
        val sheet = photoWordsState.sheet as? PhotoSheet.TextEdit ?: return noOp("updateSheetDraft: no text edit")
        logStep("updateSheetDraft", "length" to text.length)
        return copy(photoWordsState = photoWordsState.copy(sheet = sheet.copy(draft = text))).begin()
    }

    /**
     * Поднять флаг «были изменения» (AWP-19): удаление, правка текста или
     * смена части речи. Выбор групп, соответствий и «куда класть» флага
     * не ставит (AWP-19.6, 19.8). Эффектов нет.
     */
    fun WordsTabState.markDirty(): WordsResult {
        if (photoWordsState.isDirty) return noOp("markDirty: already dirty")
        logStep("markDirty")
        return copy(photoWordsState = photoWordsState.copy(isDirty = true)).begin()
    }

    /** ###### ДОБАВЛЕНИЕ (Ш4, В3) ###### */

    /** Показать вопрос «Добавить N значений в M слов?» (AWP-13). Эффектов нет. */
    fun WordsTabState.askAddConfirm(): WordsResult {
        logStep("askAddConfirm", "lexemes" to photoWordsState.summary.lexemeCount, "words" to photoWordsState.summary.wordCount)
        return copy(photoWordsState = photoWordsState.copy(isConfirmingAdd = true)).begin()
    }

    /** Убрать вопрос о добавлении («да» или «отмена»). Эффектов нет. */
    fun WordsTabState.closeAddConfirm(): WordsResult {
        if (!photoWordsState.isConfirmingAdd) return noOp("closeAddConfirm: not asking")
        logStep("closeAddConfirm")
        return copy(photoWordsState = photoWordsState.copy(isConfirmingAdd = false)).begin()
    }

    /**
     * Начать запись (AWP-13.4, AWP-20): окно неактивно, прежний сбой
     * снят, пакет из содержимого окна уходит эффектом — ему нужны окно
     * целиком и номер запуска.
     */
    fun WordsTabState.startAdding(): WordsResult {
        val batch = photoWordsState.toWordsBatch()
        logStep("startAdding", "words" to batch.words.size, "lexemes" to batch.words.sumOf { it.lexemes.size })
        return copy(photoWordsState = photoWordsState.copy(isAdding = true, addFailure = null)) to setOf(
            DatasourceEffect.AddPhotoWords(batch = batch, requestId = photoWordsState.requestId),
        )
    }

    /**
     * Запись не удалась (В3, AWP-21): окно снова активно, причина — в
     * [PhotoWordsState.addFailure]. Сбросы недействительного выбора —
     * отдельные атомы цепочки.
     * @param failure причина.
     * Эффектов нет.
     */
    fun WordsTabState.showAddFailure(failure: PhotoAddFailure): WordsResult {
        logStep("showAddFailure", "failure" to failure)
        return copy(photoWordsState = photoWordsState.copy(isAdding = false, addFailure = failure)).begin()
    }

    /** Снять «не добавилось» перед повтором (AWP-26). No-op без сбоя. Эффектов нет. */
    fun WordsTabState.clearAddFailure(): WordsResult {
        photoWordsState.addFailure ?: return noOp("clearAddFailure: no failure")
        logStep("clearAddFailure")
        return copy(photoWordsState = photoWordsState.copy(addFailure = null)).begin()
    }

    /**
     * Удалённая группа (AWP-21.3, 21.5): общая → «Все», своя у карточек →
     * «как у всех», из формы она уходит.
     * @param groupId id удалённой группы.
     * Эффектов нет.
     */
    fun WordsTabState.resetDeadGroup(groupId: Long): WordsResult {
        logStep("resetDeadGroup", "groupId" to groupId)
        val photo = photoWordsState
        return copy(
            photoWordsState = photo.copy(
                commonGroupId = photo.commonGroupId?.takeIf { it != groupId },
                cards = photo.cards.map { card ->
                    if (card.ownGroup == PhotoOwnGroup.Group(groupId)) card.copy(ownGroup = PhotoOwnGroup.AsCommon) else card
                },
                form = photo.form?.let { form -> form.copy(groups = form.groups.filterNot { it.id == groupId }) },
            ),
        ).begin()
    }

    /**
     * Удалённая лемма словаря (AWP-21.4): «куда класть» у карточек с ней →
     * «новое слово», из формы она уходит.
     * @param wordId id удалённой леммы.
     * Эффектов нет.
     */
    fun WordsTabState.resetDeadTarget(wordId: Long): WordsResult {
        logStep("resetDeadTarget", "wordId" to wordId)
        val photo = photoWordsState
        return copy(
            photoWordsState = photo.copy(
                cards = photo.cards.map { card ->
                    if (card.target == PhotoCardTarget.Existing(wordId)) card.copy(target = PhotoCardTarget.NewWord) else card
                },
                form = photo.form?.let { form -> form.copy(existingLemmas = form.existingLemmas.filterNot { it.wordId == wordId }) },
            ),
        ).begin()
    }

    /**
     * Выключенный или удалённый компонент (AWP-21.6): уходит из формы, его
     * цели — к умолчаниям вида (AWP-06.5). Пометки «ядро» и сводку
     * пересчитывают следующие атомы цепочки.
     * @param typeId id компонента.
     * No-op без формы. Эффектов нет.
     */
    fun WordsTabState.dropComponent(typeId: Long): WordsResult {
        val form = photoWordsState.form ?: return noOp("dropComponent: no form")
        val narrowed = form.copy(components = form.components.filterNot { it.id == typeId })
        logStep("dropComponent", "typeId" to typeId, "mapping" to photoWordsState.mapping)
        return copy(
            photoWordsState = photoWordsState.copy(form = narrowed, mapping = narrowed.revalidate(photoWordsState.mapping)),
        ).begin()
    }

    /** ###### ЗАКРЫТИЕ (В2) ###### */

    /** Показать вопрос «Закрыть без добавления?» (AWP-25). Эффектов нет. */
    fun WordsTabState.askCloseConfirm(): WordsResult {
        logStep("askCloseConfirm")
        return copy(photoWordsState = photoWordsState.copy(isConfirmingClose = true)).begin()
    }

    /** Убрать вопрос о закрытии («остаться»). No-op, если вопроса нет. Эффектов нет. */
    fun WordsTabState.closeCloseConfirm(): WordsResult {
        if (!photoWordsState.isConfirmingClose) return noOp("closeCloseConfirm: not asking")
        logStep("closeCloseConfirm")
        return copy(photoWordsState = photoWordsState.copy(isConfirmingClose = false)).begin()
    }

    /** ###### ВНУТРЕННИЕ ХЕЛПЕРЫ (чистые, не шаги — не логируются): общие переходы модуля candidates над карточками ###### */

    private fun WordsTabState.cardOf(lemma: String): PhotoCard? = photoWordsState.cards.cardOf(lemma)

    private fun WordsTabState.lexemeOf(
        lemma: String,
        key: String,
    ): PhotoCardLexeme? = photoWordsState.cards.lexemeOf(lemma, key)

    private fun WordsTabState.updateCard(
        lemma: String,
        change: (PhotoCard) -> PhotoCard,
    ): WordsResult = copy(photoWordsState = photoWordsState.copy(cards = photoWordsState.cards.updateCard(lemma, change))).begin()

    private fun WordsTabState.updateLexeme(
        lemma: String,
        key: String,
        change: (PhotoCardLexeme) -> PhotoCardLexeme,
    ): WordsResult = copy(photoWordsState = photoWordsState.copy(cards = photoWordsState.cards.updateLexeme(lemma, key, change))).begin()
}
