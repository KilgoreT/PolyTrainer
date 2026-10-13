package me.apomazkin.wordstab.logic.photo

import me.apomazkin.lexeme.lexemeTextKey
import me.apomazkin.lexeme.wordTextKey
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoExistingLemma
import me.apomazkin.wordstab.deps.PhotoLemma
import me.apomazkin.wordstab.deps.PhotoLexeme
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.deps.PhotoWordStatus

/*
 * IS532: сборка карточек из ответа сервера и формы словаря (Ш1) и
 * пересчёт пометок (Ш3) — чистые функции, которые зовут атомы reducer'а.
 */

/** Сколько лексем-кандидатов показано в карточке (AWP-05). */
internal const val VISIBLE_LEXEMES = 5

/** Язык перевода, при котором толкование и есть перевод — пометки «англ.» нет (AWP-10.4). */
internal const val ENGLISH = "en"

/** Карточки и слова без добавления из ответа (AWP-04, AWP-12). */
internal data class PhotoCardsBuild(
        val cards: List<PhotoCard>,
        val notAdded: List<PhotoNotAdded>,
)

/**
 * По карточке на лемму в порядке первого появления (AWP-04): одна и та
 * же лемма у разных слов — одна карточка со всеми словами; лемма без
 * лексем-кандидатов карточки не получает; слово, у которого все леммы
 * такие, «не найдено» и «ошибка источника» — в «не добавить» (AWP-12).
 */
internal fun buildPhotoCards(words: List<PhotoWord>, form: PhotoDictionaryForm): PhotoCardsBuild {
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
    )
}

/** Леммы словаря с тем же текстом (AWP-07.3): без регистра, краёв и формы Unicode; порядок формы — самая новая первой. */
internal fun PhotoDictionaryForm.existingOf(lemma: String): List<PhotoExistingLemma> {
    val key = wordTextKey(lemma)
    return existingLemmas.filter { wordTextKey(it.text) == key }
}

/**
 * Пять лексем-кандидатов (AWP-05): по одной на каждую часть речи в порядке
 * сервера, затем добор до пяти по порядку сервера; «без части речи» —
 * тоже часть.
 */
internal fun visibleLexemes(lexemes: List<PhotoLexeme>): List<PhotoLexeme> {
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
internal fun List<PhotoCard>.withExistsMarks(form: PhotoDictionaryForm): List<PhotoCard> = map { card ->
    val keys = form.existingOf(card.lemma).flatMapTo(HashSet()) { it.lexemeTextKeys }
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(exists = lexeme.matches(keys)) })
}

private fun PhotoCardLexeme.matches(keys: Set<String>): Boolean {
    if (keys.isEmpty()) return false
    val texts = listOfNotNull(translation.text, gloss?.text).filter { it.isNotBlank() }
    return texts.any { lexemeTextKey(it) in keys }
}

/**
 * Пометка «не попадёт в ядро» (AWP-11): при текущих соответствиях ни один
 * включённый компонент-ядро не получит текста; считается тем же правилом,
 * что и запись (AWP-22, [componentsFor]).
 */
internal fun List<PhotoCard>.withCoreMarks(mapping: PhotoMapping, form: PhotoDictionaryForm): List<PhotoCard> = map { card ->
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(lacksCore = !lexeme.fillsCore(mapping, form)) })
}

internal fun PhotoCardLexeme.fillsCore(mapping: PhotoMapping, form: PhotoDictionaryForm): Boolean =
    componentsFor(mapping, form).any { component -> form.components.any { it.id == component.typeId && it.isCore } }

/**
 * Сводка кнопки «добавить» (AWP-13): лексемы без «есть» и слова, в которые
 * они лягут; «есть» отдельно; без ядра — причина неактивности; ни один
 * вид данных не идёт в ядро — сводка красная (AWP-11.7).
 */
internal fun summaryOf(cards: List<PhotoCard>, mapping: PhotoMapping, form: PhotoDictionaryForm?): PhotoAddSummary {
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
