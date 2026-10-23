package me.apomazkin.candidates

import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.lexeme.wordTextKey

/*
 * IS532/IS536: сборка карточек из ответа сервера и формы словаря (Ш1) и
 * пересчёт пометок (Ш3) — чистые функции, которые зовут атомы редьюсеров
 * обоих окон. Сборка, пометки и сводка идут по сопоставлению (AWP-29).
 */

/**
 * По карточке на лемму в порядке первого появления (AWP-04): одна и та
 * же лемма у разных слов — одна карточка со всеми словами и лексемами
 * без повторов ключа, лексема — целиком из первого слова (AWP-04.8);
 * лемма без лексем-кандидатов карточки не получает; слово, у которого
 * все леммы такие, «не найдено» и «ошибка» — в «не добавить» (AWP-12).
 */
fun buildLemmaCards(
    words: List<LookupWordUi>,
    form: DictionaryFormUi,
): CardsBuildUi {
    val cards = LinkedHashMap<String, LemmaCardUiItem>()
    val notAdded = mutableListOf<NotAddedWordUiItem>()
    for (word in words) {
        when (word.status) {
            LookupWordStatus.FOUND -> {
                val lemmas = word.lemmas.filter { it.lexemes.isNotEmpty() }
                if (lemmas.isEmpty()) notAdded += NotAddedWordUiItem(word.text, NotAddedReason.NO_LEXEMES)
                for (lemma in lemmas) {
                    val card = cards[lemma.text]
                    cards[lemma.text] = card?.withLemma(lemma, word, form) ?: newCard(lemma, word, form)
                }
            }
            LookupWordStatus.NOT_FOUND -> notAdded += NotAddedWordUiItem(word.text, NotAddedReason.NOT_FOUND)
            LookupWordStatus.ERROR -> notAdded += NotAddedWordUiItem(word.text, NotAddedReason.ERROR)
        }
    }
    return CardsBuildUi(cards = cards.values.toList(), notAdded = notAdded)
}

/** Та же лемма у ещё одного слова: слово в список, новые ключи лексем в конец (AWP-04.3, 04.8). */
private fun LemmaCardUiItem.withLemma(
    lemma: LookupLemmaUi,
    word: LookupWordUi,
    form: DictionaryFormUi,
): LemmaCardUiItem {
    val known = lexemes.mapTo(HashSet()) { it.key }
    val fresh = lemma.lexemes.filterNot { it.key in known }.map { it.toCardLexeme(lemma, word, form) }
    return copy(
        sourceWords = if (word.text in sourceWords) sourceWords else sourceWords + word.text,
        lexemes = lexemes + fresh,
    )
}

/** Карточка леммы: лексемы по сопоставлению, «есть в словаре: N», умолчание «куда класть» (AWP-18.3). */
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

/** Лексема-кандидат в окне: значения по сопоставлению (AWP-29), «англ.» по замечанию слова (AWP-34). */
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
)

private fun WordRemarkUi.marksLexeme(
    lemma: String,
    key: String,
): Boolean = this is WordRemarkUi.NoTranslation && this.lemma == lemma && this.lexemeKey == key

/** Леммы словаря с тем же текстом (AWP-07.3): без регистра, краёв и формы Unicode; порядок формы — самая новая первой. */
fun DictionaryFormUi.existingOf(lemma: String): List<ExistingLemmaUi> {
    val key = wordTextKey(lemma)
    return existingLemmas.filter { wordTextKey(it.text) == key }
}

/** Ключи текстов ядер всех лексем всех слов словаря с текстом леммы (AWP-07, WCL-15.4). */
fun DictionaryFormUi.lemmaWordKeys(lemma: String): Set<String> = existingOf(lemma).flatMapTo(HashSet()) { it.lexemeTextKeys }

/**
 * Пометка «есть» (AWP-07, Д42): значение активного ядра лексемы-кандидата
 * совпадает со значением ядра шаблона «текст» любой лексемы лемм словаря с
 * тем же текстом; сравнение — `lexemeTextKey`; параметры не сравниваются.
 */
fun List<LemmaCardUiItem>.withExistsMarks(form: DictionaryFormUi): List<LemmaCardUiItem> = map { card ->
    val keys = form.lemmaWordKeys(card.lemma)
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(exists = lexeme.matchesTexts(keys, form)) })
}

/** Пометка «есть» по ключам текстов ядер одного слова (WCL-06): для всех разделов, в том числе лемм-форм. */
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
 * Сводка кнопки «добавить» (AWP-13): лексемы без «есть» и слова, в которые
 * они лягут; «есть» отдельно; без ядра — причина неактивности.
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
