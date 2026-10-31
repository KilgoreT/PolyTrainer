package me.apomazkin.scancandidates.mate

import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.lexeme.TextLemmas
import me.apomazkin.lexeme.wordTextKey

/*
 * Чистые функции списков окна: без состояния и эффектов. Леммы текста из
 * домена → списки экрана, пометка «в словаре» по леммам словаря (AWP-39),
 * слово и его карточка по тексту, карточки лемм у всех слов, счёт слов с
 * недобавленными лексемами-кандидатами (AWP-51).
 */

/** Леммы текста → списки окна: по слову на строку каждого списка, без пометок. */
fun TextLemmas.toUi(): ScanWordListsUi = ScanWordListsUi(
    lexical = lexical.map { ScanWordUiItem(it) },
    proper = proper.map { ScanWordUiItem(it) },
    function = function.map { ScanWordUiItem(it) },
    unparsed = unparsed.map { ScanWordUiItem(it) },
)

/**
 * Пометка «в словаре» (AWP-39): слово помечено, когда ключ его текста — края,
 * NFC, нижний регистр — совпал с ключом текста леммы словаря; «ё» и «е»
 * разные (AWP-39.4).
 */
fun ScanWordListsUi.withDictionaryMarks(existing: List<ExistingLemmaUi>): ScanWordListsUi {
    val keys = existing.map { wordTextKey(it.text) }.toSet()
    return mapItems { it.copy(isInDictionary = wordTextKey(it.text) in keys) }
}

/** Тексты всех слов четырёх списков в порядке показа. */
fun ScanWordListsUi.allTexts(): List<String> = allItems().map { it.text }

/** Сколько слов с пометкой «в словаре», для лога. */
fun ScanWordListsUi.countInDictionary(): Int = allItems().count { it.isInDictionary }

/** Слово списков по тексту; слово с одним текстом в двух списках — первая его строка. null — его нет. */
fun ScanWordListsUi.wordOf(text: String): ScanWordUiItem? = allItems().firstOrNull { it.text == text }

/** Строки с этим текстом во всех списках с изменением; остальные как были. */
fun ScanWordListsUi.updateWord(
    text: String,
    change: (ScanWordUiItem) -> ScanWordUiItem,
): ScanWordListsUi = mapItems { if (it.text == text) change(it) else it }

/** Без строк с этим текстом (AWP-48, Д118). */
fun ScanWordListsUi.removeWord(text: String): ScanWordListsUi = filterItems { it.text != text }

/** Карточки всех слов с изменением; слово без карточки как было. */
fun ScanWordListsUi.updateCards(change: (WordCardUi) -> WordCardUi): ScanWordListsUi = mapItems { item ->
    val card = item.card ?: return@mapItems item
    item.copy(card = change(card))
}

/**
 * Карточки с этой леммой у всех слов с изменением; null от перехода у
 * карточки — она как была, null у всех — шага нет.
 */
fun ScanWordListsUi.updateLemmaCards(
    lemma: String,
    change: (LemmaCardUiItem) -> LemmaCardUiItem?,
): ScanWordListsUi? {
    var changed = false
    val next = updateCards { card ->
        if (card.lemma.lemma != lemma) return@updateCards card
        val lemmaCard = change(card.lemma) ?: return@updateCards card
        changed = true
        card.copy(lemma = lemmaCard)
    }
    return if (changed) next else null
}

/** Карточки лемм всех слов с карточкой, слово с одним текстом — один раз: для листов правки и «куда класть». */
fun ScanWordListsUi.allLemmaCards(): List<LemmaCardUiItem> = allItems()
    .distinctBy { it.text }
    .mapNotNull { it.card?.lemma }

/**
 * Слова, чьи лексемы-кандидаты идут или пришли и есть что добавить (AWP-51):
 * карточка «загрузка», или «лексемы-кандидаты» с лексемой без «уже в
 * словаре»; слово с одним текстом в двух списках считается один раз.
 */
fun ScanWordListsUi.countPending(): Int = allItems()
    .distinctBy { it.text }
    .count { it.card?.isPending() == true }

private fun WordCardUi.isPending(): Boolean = when (phase) {
    CardPhase.LOADING -> true
    CardPhase.CANDIDATES -> lemma.lexemes.any { !it.exists }
    CardPhase.SAVED,
    CardPhase.NOT_FOUND,
    CardPhase.ERROR,
    -> false
}

/** Идёт запись хотя бы одного слова. */
fun ScanWordListsUi.anyAdding(): Boolean = allItems().any { it.card?.isAdding == true }

private fun ScanWordListsUi.allItems(): List<ScanWordUiItem> =
    lexical + proper + function + unparsed

private fun ScanWordListsUi.mapItems(change: (ScanWordUiItem) -> ScanWordUiItem): ScanWordListsUi =
    ScanWordListsUi(
        lexical = lexical.map(change),
        proper = proper.map(change),
        function = function.map(change),
        unparsed = unparsed.map(change),
    )

private fun ScanWordListsUi.filterItems(keep: (ScanWordUiItem) -> Boolean): ScanWordListsUi =
    ScanWordListsUi(
        lexical = lexical.filter(keep),
        proper = proper.filter(keep),
        function = function.filter(keep),
        unparsed = unparsed.filter(keep),
    )
