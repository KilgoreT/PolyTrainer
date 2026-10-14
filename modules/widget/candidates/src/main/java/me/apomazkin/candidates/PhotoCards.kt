package me.apomazkin.candidates

import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.lexeme.wordTextKey

/*
 * IS532: сборка карточек из ответа сервера и формы словаря (Ш1) и
 * пересчёт пометок (Ш3) — чистые функции, которые зовут атомы reducer'а.
 */

/** Сколько лексем-кандидатов показано в карточке (AWP-05). */
const val VISIBLE_LEXEMES = 5

/** Язык перевода, при котором толкование и есть перевод — пометки «англ.» нет (AWP-10.4). */
const val ENGLISH = "en"

/** Карточки и слова без добавления из ответа (AWP-04, AWP-12). */
data class PhotoCardsBuild(
        val cards: List<PhotoCard>,
        val notAdded: List<PhotoNotAdded>,
)

/**
 * По карточке на лемму в порядке первого появления (AWP-04): одна и та
 * же лемма у разных слов — одна карточка со всеми словами; лемма без
 * лексем-кандидатов карточки не получает; слово, у которого все леммы
 * такие, «не найдено» и «ошибка источника» — в «не добавить» (AWP-12).
 */
fun buildPhotoCards(words: List<PhotoWord>, form: PhotoDictionaryForm): PhotoCardsBuild {
    val cards = LinkedHashMap<String, PhotoCard>()
    val notAdded = mutableListOf<PhotoNotAdded>()
    for (word in words) {
        when (word.status) {
            PhotoWordStatus.FOUND -> {
                val lemmas = word.lemmas.filter { it.lexemes.isNotEmpty() }
                if (lemmas.isEmpty()) notAdded += PhotoNotAdded(word.text, PhotoNotAddedReason.NO_LEXEMES)
                for (lemma in lemmas) {
                    val card = cards[lemma.text]
                    cards[lemma.text] = card?.withSourceWord(word.text) ?: newCard(lemma, word.text, form)
                }
            }
            PhotoWordStatus.NOT_FOUND -> notAdded += PhotoNotAdded(word.text, PhotoNotAddedReason.NOT_FOUND)
            PhotoWordStatus.ERROR -> notAdded += PhotoNotAdded(word.text, PhotoNotAddedReason.ERROR)
        }
    }
    return PhotoCardsBuild(cards = cards.values.toList(), notAdded = notAdded)
}

private fun PhotoCard.withSourceWord(word: String): PhotoCard =
    if (word in sourceWords) this else copy(sourceWords = sourceWords + word)

/** Карточка леммы: пять лексем, скрытые числом, «есть в словаре: N», умолчание «куда класть» (AWP-18.3). */
private fun newCard(lemma: PhotoLemma, word: String, form: PhotoDictionaryForm): PhotoCard {
    val existing = form.existingOf(lemma.text)
    val visible = visibleLexemes(lemma.lexemes)
    return PhotoCard(
            lemma = lemma.text,
            sourceWords = listOf(word),
            lexemes = visible.map { it.toCardLexeme(form) },
            hiddenCount = lemma.lexemes.size - visible.size,
            existingCount = existing.size,
            target = existing.firstOrNull()?.let { PhotoCardTarget.Existing(it.wordId) } ?: PhotoCardTarget.NewWord,
            ownGroup = PhotoOwnGroup.AsCommon,
            isExpanded = false,
            formNote = lemma.formNote,
    )
}

/**
 * Разделы диалога в карточке слова (WCL-04): по карточке на лемму ответа с
 * лексемами, прямая лемма — с текстом слова без учёта регистра и краёв —
 * первой и без пометки формы, остальные — леммы-формы с пометкой сервера;
 * все разделы развёрнуты, цель — слово диалога, группы нет.
 */
fun buildCandidateCards(word: PhotoWord, wordText: String, form: PhotoDictionaryForm): List<PhotoCard> {
    val key = wordTextKey(wordText)
    val cards = word.lemmas
            .filter { it.lexemes.isNotEmpty() }
            .map { lemma ->
                val isDirect = wordTextKey(lemma.text) == key
                newCard(lemma, word.text, form).copy(
                        isExpanded = true,
                        target = PhotoCardTarget.NewWord,
                        formNote = if (isDirect) null else lemma.formNote,
                )
            }
    return cards.sortedBy { if (wordTextKey(it.lemma) == key) 0 else 1 }
}

/**
 * Пометка «есть» по ключам текстов лексем одного слова (WCL-06): для всех
 * разделов, в том числе лемм-форм; сравнение — `lexemeTextKey`.
 */
fun List<PhotoCard>.withExistsMarks(keys: Set<String>): List<PhotoCard> = map { card ->
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(exists = lexeme.matchesTexts(keys)) })
}

/**
 * Пометка «есть в «correr»» (WCL-15.4): у раздела леммы-формы — текст
 * перевода или толкование кандидата совпадает со значением шаблона «текст»
 * любого слова словаря с текстом этой леммы; у прямой леммы пометки нет —
 * её слово и есть слово диалога (WCL-06).
 */
fun List<PhotoCard>.withLemmaExistsMarks(form: PhotoDictionaryForm): List<PhotoCard> = map { card ->
    val keys = if (card.formNote == null) emptySet() else form.lemmaWordKeys(card.lemma)
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(existsInLemma = lexeme.matchesTexts(keys)) })
}

/** Ключи текстов лексем всех слов словаря с текстом леммы (WCL-15.4). */
fun PhotoDictionaryForm.lemmaWordKeys(lemma: String): Set<String> = existingOf(lemma).flatMapTo(HashSet()) { it.lexemeTextKeys }

/**
 * Сколько значений у слов леммы-формы совпадёт с тем, что окажется в одном
 * слове (WCL-15.4): кандидаты раздела с пометкой «есть в …» плюс тексты
 * лексем слова диалога [ownKeys], которые есть и у слов леммы. Леммы без
 * совпадений в карте нет.
 */
fun sameValuesOf(cards: List<PhotoCard>, form: PhotoDictionaryForm, ownKeys: Set<String>): Map<String, Int> = cards
        .filter { it.formNote != null }
        .associate { card ->
            val lemmaKeys = form.lemmaWordKeys(card.lemma)
            card.lemma to card.lexemes.count { it.existsInLemma } + ownKeys.count { it in lemmaKeys }
        }
        .filterValues { it > 0 }

/** Леммы словаря с тем же текстом (AWP-07.3): без регистра, краёв и формы Unicode; порядок формы — самая новая первой. */
fun PhotoDictionaryForm.existingOf(lemma: String): List<PhotoExistingLemma> {
    val key = wordTextKey(lemma)
    return existingLemmas.filter { wordTextKey(it.text) == key }
}

/**
 * Пять лексем-кандидатов (AWP-05): по одной на каждую часть речи в порядке
 * сервера, затем добор до пяти по порядку сервера; «без части речи» —
 * тоже часть.
 */
fun visibleLexemes(lexemes: List<PhotoLexeme>): List<PhotoLexeme> {
    val picked = LinkedHashSet<String>()
    val seenParts = mutableSetOf<String?>()
    for (lexeme in lexemes) {
        if (picked.size >= VISIBLE_LEXEMES) break
        if (seenParts.add(lexeme.partOfSpeechKey)) picked += lexeme.key
    }
    for (lexeme in lexemes) {
        if (picked.size >= VISIBLE_LEXEMES) break
        picked += lexeme.key
    }
    return lexemes.filter { it.key in picked }
}

/**
 * Строка лексемы-кандидата (AWP-10): переводы через «, »; без переводов —
 * толкование как текст с пометкой «англ.», если язык перевода не
 * английский; толкование второй строкой — только когда оно не стало
 * текстом; часть речи — опция по ключу (AWP-17.2).
 */
private fun PhotoLexeme.toCardLexeme(form: PhotoDictionaryForm): PhotoCardLexeme {
    val hasOwnTranslations = translations.isNotEmpty()
    val text = if (hasOwnTranslations) translations.joinToString(", ") else gloss.orEmpty()
    return PhotoCardLexeme(
            key = key,
            source = source,
            translation = PhotoEditableText.of(text),
            hasOwnTranslations = hasOwnTranslations,
            isEnglishFallback = !hasOwnTranslations && gloss != null && form.translationLanguage != ENGLISH,
            gloss = gloss?.takeIf { hasOwnTranslations }?.let { PhotoEditableText.of(it) },
            caption = caption,
            notes = notes,
            examples = examples.map { PhotoEditableText.of(it) },
            partOfSpeechOptionId = form.partOfSpeechOptions.firstOrNull { it.key != null && it.key == partOfSpeechKey }?.id,
            parent = parent,
            parentKey = parentKey,
            exists = false,
            lacksCore = false,
    )
}

/**
 * Пометка «есть» (AWP-07): текст перевода или толкование лексемы-кандидата
 * совпадает с любым значением шаблона «текст» любой лексемы лемм словаря
 * с тем же текстом; сравнение — `lexemeTextKey`; от соответствий не зависит.
 */
fun List<PhotoCard>.withExistsMarks(form: PhotoDictionaryForm): List<PhotoCard> = map { card ->
    val keys = form.lemmaWordKeys(card.lemma)
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(exists = lexeme.matchesTexts(keys)) })
}

/** Текст перевода или толкование кандидата совпадает с одним из ключей текстов (сравнение — `lexemeTextKey`). */
fun PhotoCardLexeme.matchesTexts(keys: Set<String>): Boolean {
    if (keys.isEmpty()) return false
    val texts = listOfNotNull(translation.text, gloss?.text).filter { it.isNotBlank() }
    return texts.any { lexemeTextKey(it) in keys }
}

/**
 * Пометка «не попадёт в ядро» (AWP-11): при текущих соответствиях ни один
 * включённый компонент-ядро не получит текста; считается тем же правилом,
 * что и запись (AWP-22, [componentsFor]).
 */
fun List<PhotoCard>.withCoreMarks(mapping: PhotoMapping, form: PhotoDictionaryForm): List<PhotoCard> = map { card ->
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(lacksCore = !lexeme.fillsCore(mapping, form)) })
}

fun PhotoCardLexeme.fillsCore(mapping: PhotoMapping, form: PhotoDictionaryForm): Boolean =
    componentsFor(mapping, form).any { component -> form.components.any { it.id == component.typeId && it.isCore } }

/**
 * Сводка кнопки «добавить» (AWP-13): лексемы без «есть» и слова, в которые
 * они лягут; «есть» отдельно; без ядра — причина неактивности; ни один
 * вид данных не идёт в ядро — сводка красная (AWP-11.7).
 */
fun summaryOf(cards: List<PhotoCard>, mapping: PhotoMapping, form: PhotoDictionaryForm?): PhotoAddSummary {
    val lexemes = cards.flatMap { it.lexemes }
    val toAdd = lexemes.filterNot { it.exists }
    val lacksCore = toAdd.count { it.lacksCore }
    return PhotoAddSummary(
            lexemeCount = toAdd.size,
            wordCount = cards.count { card -> card.lexemes.any { !it.exists } },
            existingCount = lexemes.count { it.exists },
            lacksCoreCount = lacksCore,
            noCoreMapping = form != null && mapping.hasNoCore(form),
            isEnabled = toAdd.isNotEmpty() && lacksCore == 0,
    )
}
