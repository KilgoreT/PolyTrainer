package me.apomazkin.candidates

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.lexeme.wordTextKey

/*
 * IS532/IS536/IS538: карточка леммы из ответа сервера и формы словаря (Ш6),
 * разделы диалога карточки слова (WCL-04) и пересчёт пометок (Ш8) — чистые
 * функции, которые зовут атомы редьюсеров обоих окон. Сборка, пометки и
 * сводка идут по сопоставлению (AWP-29).
 */

/**
 * Карточка леммы без лексем по форме: леммы словаря с тем же текстом, «куда
 * класть» — самая новая или «новое слово» (AWP-18.3), своя группа «как у
 * всех». Карточка слова «в словаре» до проверки по тексту и прежняя карточка
 * нового слова до ответа.
 */
fun emptyLemmaCard(
    lemma: String,
    form: DictionaryFormUi,
): LemmaCardUiItem {
    val existing = form.existingOf(lemma)
    return LemmaCardUiItem(
        lemma = lemma,
        sourceWords = listOf(lemma),
        lexemes = emptyList(),
        existingCount = existing.size,
        target = existing.firstOrNull()?.let { CardTargetUi.Existing(it.wordId) } ?: CardTargetUi.NewWord,
        ownGroup = CardOwnGroupUi.AsCommon,
    )
}

/**
 * Карточка одной леммы из слова ответа: первая лемма с лексемами (по тапу
 * она одна, LFL-107), лексемы по сопоставлению в порядке карточки (AWP-44),
 * с «в тексте» и «англ.», пометка формы; «куда класть» и своя группа — от
 * прежней карточки [prior], если текст леммы тот же, иначе по форме заново
 * (AWP-43.7). Слово без леммы с лексемами — прежняя карточка без лексем.
 */
fun lemmaCardOf(
    word: LookupWordUi,
    prior: LemmaCardUiItem,
    form: DictionaryFormUi,
): LemmaCardUiItem {
    val lemma = word.lemmas.firstOrNull { it.lexemes.isNotEmpty() }
        ?: return prior.copy(sourceWords = listOf(word.text), lexemes = emptyList())
    val base = when (wordTextKey(lemma.text)) {
        wordTextKey(prior.lemma) -> prior
        else -> emptyLemmaCard(lemma.text, form)
    }
    return base.copy(
        sourceWords = listOf(word.text),
        lexemes = lemma.lexemes.map { it.toCardLexeme(lemma, word, form).inCardOrder(form) },
        formNote = lemma.formNote,
    )
}

/**
 * Разделы диалога в карточке слова (WCL-04): по карточке на лемму ответа с
 * лексемами, прямая лемма — с текстом слова без учёта регистра и краёв —
 * первой и без пометки формы, остальные — леммы-формы с пометкой сервера;
 * цель — слово диалога, группы нет.
 */
fun buildCandidateCards(
    word: LookupWordUi,
    wordText: String,
    form: DictionaryFormUi,
): List<LemmaCardUiItem> {
    val key = wordTextKey(wordText)
    val cards = word.lemmas
        .filter { it.lexemes.isNotEmpty() }
        .map { lemma ->
            val isDirect = wordTextKey(lemma.text) == key
            newCard(lemma, word, form).copy(
                target = CardTargetUi.NewWord,
                formNote = if (isDirect) null else lemma.formNote,
            )
        }
    return cards.sortedBy { if (wordTextKey(it.lemma) == key) 0 else 1 }
}

/** Раздел леммы диалога: лексемы по сопоставлению, «есть в словаре: N», умолчание «куда класть». */
private fun newCard(
    lemma: LookupLemmaUi,
    word: LookupWordUi,
    form: DictionaryFormUi,
): LemmaCardUiItem {
    val existing = form.existingOf(lemma.text)
    return LemmaCardUiItem(
        lemma = lemma.text,
        sourceWords = listOf(word.text),
        lexemes = lemma.lexemes.map { it.toCardLexeme(lemma, word, form) },
        existingCount = existing.size,
        target = existing.firstOrNull()?.let { CardTargetUi.Existing(it.wordId) } ?: CardTargetUi.NewWord,
        ownGroup = CardOwnGroupUi.AsCommon,
        formNote = lemma.formNote,
    )
}

/** Лексема-кандидат в окне: значения по сопоставлению (AWP-29), «англ.» по замечанию слова (AWP-34), «в тексте» (AWP-43). */
private fun LookupLexemeUi.toCardLexeme(
    lemma: LookupLemmaUi,
    word: LookupWordUi,
    form: DictionaryFormUi,
): CardLexemeUiItem = CardLexemeUiItem(
    key = key,
    source = source,
    values = valuesOf(this, lemma, form),
    isEnglishFallback = word.remarks.any { it.marksLexeme(lemma.text, key) },
    exists = false,
    lacksCore = false,
    isInContext = inContext,
)

private fun WordRemarkUi.marksLexeme(
    lemma: String,
    key: String,
): Boolean = this is WordRemarkUi.NoTranslation && this.lemma == lemma && this.lexemeKey == key

/** Лексема со значениями в порядке карточки под словом (AWP-44). */
fun CardLexemeUiItem.inCardOrder(form: DictionaryFormUi): CardLexemeUiItem = copy(values = values.inCardOrder(form))

/**
 * Значения в порядке карточки под словом (AWP-44): ядра, затем параметры,
 * затем «текст с подписью»; внутри части — порядок формы, зависимые сразу за
 * своим (AWP-44.3). Диалог карточки слова её не зовёт: там порядок формы
 * (WCL-20). Значения компонентов не из формы — в конце.
 */
fun List<CandidateValueUiItem>.inCardOrder(form: DictionaryFormUi): List<CandidateValueUiItem> {
    val rank = form.components.withIndex().associate { (index, component) -> component.id to (component.cardPart() to index) }
    return sortedWith(
        compareBy(
            { rank[it.componentId]?.first ?: Int.MAX_VALUE },
            { rank[it.componentId]?.second ?: Int.MAX_VALUE },
        ),
    )
}

/** Часть карточки: ядра — 0, параметры — 1, «текст с подписью» — 2 (AWP-44). */
private fun FormComponentUi.cardPart(): Int = when {
    isCore -> 0
    template == ComponentTemplate.CAPTIONED_TEXT -> 2
    else -> 1
}

/** Леммы словаря с тем же текстом (AWP-39, AWP-18): без регистра, краёв и формы Unicode; порядок формы — самая новая первой. */
fun DictionaryFormUi.existingOf(lemma: String): List<ExistingLemmaUi> {
    val key = wordTextKey(lemma)
    return existingLemmas.filter { wordTextKey(it.text) == key }
}

/** Строки листа «куда класть» для леммы (AWP-18): по лемме словаря с тем же текстом, самая новая первой. */
fun DictionaryFormUi.targetRowsOf(lemma: String): List<TargetRowUi> = existingOf(lemma).map {
    TargetRowUi(
        wordId = it.wordId,
        coreValues = it.coreValues,
        lexemeCount = it.lexemeCount,
        groupNames = it.groupNames,
        addedAt = it.addedAt,
    )
}

/** Сохранённые лексемы леммы словаря из «куда класть» со значениями в порядке карточки (AWP-46, AWP-44); «новое слово» — пусто. */
fun DictionaryFormUi.savedLexemesOf(target: CardTargetUi): List<SavedLexemeUi> = when (target) {
    is CardTargetUi.Existing -> existingLemmas
        .firstOrNull { it.wordId == target.wordId }
        ?.lexemes
        .orEmpty()
        .map { saved -> saved.copy(values = saved.values.inCardOrder(this)) }
    CardTargetUi.NewWord -> emptyList()
}

/** Ключи текстов ядер лексем леммы словаря из «куда класть» для «уже в словаре» (AWP-50); «новое слово» — пусто. */
fun DictionaryFormUi.lexemeKeysOf(target: CardTargetUi): Set<String> = when (target) {
    is CardTargetUi.Existing -> existingLemmas.firstOrNull { it.wordId == target.wordId }?.lexemeTextKeys.orEmpty()
    CardTargetUi.NewWord -> emptySet()
}

/** Форма с леммами словаря, которых в ней не было: лемма ответа вне списков (AWP-43.7). */
fun DictionaryFormUi.withExistingLemmas(added: List<ExistingLemmaUi>): DictionaryFormUi {
    val known = existingLemmas.mapTo(HashSet()) { it.wordId }
    val fresh = added.filterNot { it.wordId in known }
    return if (fresh.isEmpty()) this else copy(existingLemmas = existingLemmas + fresh)
}

/** Ключи текстов ядер всех лексем всех слов словаря с текстом леммы (WCL-15.4). */
fun DictionaryFormUi.lemmaWordKeys(lemma: String): Set<String> = existingOf(lemma).flatMapTo(HashSet()) { it.lexemeTextKeys }

/**
 * Пометка «уже в словаре» или «есть» по ключам текстов ядер одной леммы
 * словаря (AWP-50, WCL-06, Д42): значение активного ядра лексемы-кандидата
 * совпадает со значением ядра шаблона «текст» лексемы; сравнение —
 * `lexemeTextKey`; параметры не сравниваются.
 */
fun List<LemmaCardUiItem>.withExistsMarks(
    keys: Set<String>,
    form: DictionaryFormUi,
): List<LemmaCardUiItem> = map { card ->
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(exists = lexeme.matchesTexts(keys, form)) })
}

/**
 * Пометка «есть в «correr»» (WCL-15.4): у раздела леммы-формы — ядро
 * кандидата совпадает со значением ядра любого слова словаря с текстом
 * этой леммы; у прямой леммы пометки нет — её слово и есть слово диалога.
 */
fun List<LemmaCardUiItem>.withLemmaExistsMarks(form: DictionaryFormUi): List<LemmaCardUiItem> = map { card ->
    val keys = if (card.formNote == null) emptySet() else form.lemmaWordKeys(card.lemma)
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(existsInLemma = lexeme.matchesTexts(keys, form)) })
}

/**
 * Сколько значений у слов леммы-формы совпадёт с тем, что окажется в одном
 * слове (WCL-15.4): кандидаты раздела с пометкой «есть в …» плюс тексты
 * лексем слова диалога [ownKeys], которые есть и у слов леммы.
 */
fun sameValuesOf(
    cards: List<LemmaCardUiItem>,
    form: DictionaryFormUi,
    ownKeys: Set<String>,
): Map<String, Int> = cards
    .filter { it.formNote != null }
    .associate { card ->
        val lemmaKeys = form.lemmaWordKeys(card.lemma)
        card.lemma to card.lexemes.count { it.existsInLemma } + ownKeys.count { it in lemmaKeys }
    }
    .filterValues { it > 0 }

/** Ключи непустых текстов активных ядер лексемы-кандидата (сравнение — `lexemeTextKey`). */
fun CardLexemeUiItem.coreTextKeys(form: DictionaryFormUi): Set<String> = values
    .filterIsInstance<CandidateValueUiItem.Text>()
    .filter { it.isActive && form.componentOf(it.componentId)?.isCore == true }
    .flatMap { value -> value.texts.map { it.text } }
    .filter { it.isNotBlank() }
    .mapTo(HashSet()) { lexemeTextKey(it) }

/** Ядро кандидата совпадает с одним из ключей текстов. */
fun CardLexemeUiItem.matchesTexts(
    keys: Set<String>,
    form: DictionaryFormUi,
): Boolean = keys.isNotEmpty() && coreTextKeys(form).any { it in keys }

/** Пометка «не попадёт в ядро» (AWP-11): ни одно активное ядро не имеет содержимого. */
fun List<LemmaCardUiItem>.withCoreMarks(form: DictionaryFormUi): List<LemmaCardUiItem> = map { card ->
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(lacksCore = !lexeme.fillsCore(form)) })
}

fun CardLexemeUiItem.fillsCore(form: DictionaryFormUi): Boolean =
    values.any { it.isActive && it.hasContent && form.componentOf(it.componentId)?.isCore == true }

/**
 * Сводка кнопки «добавить» диалога (WCL-10): лексемы без «есть» и слова, в
 * которые они лягут; «есть» отдельно; без ядра — причина неактивности.
 */
fun summaryOf(cards: List<LemmaCardUiItem>): AddSummaryUi {
    val lexemes = cards.flatMap { it.lexemes }
    val toAdd = lexemes.filterNot { it.exists }
    val lacksCore = toAdd.count { it.lacksCore }
    return AddSummaryUi(
        lexemeCount = toAdd.size,
        wordCount = cards.count { card -> card.lexemes.any { !it.exists } },
        existingCount = lexemes.count { it.exists },
        lacksCoreCount = lacksCore,
        isEnabled = toAdd.isNotEmpty() && lacksCore == 0,
    )
}
