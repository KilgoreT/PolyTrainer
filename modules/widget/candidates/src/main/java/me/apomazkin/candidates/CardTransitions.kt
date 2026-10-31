package me.apomazkin.candidates

/*
 * IS533/IS536: переходы над списком карточек — чистые функции, общие для
 * окна снимка и диалога в карточке слова. Атомы экранов оборачивают их
 * логом и эффектами; null — «шага нет», атом пишет no-op.
 */

/** Карточка по тексту леммы. */
fun List<LemmaCardUiItem>.cardOf(lemma: String): LemmaCardUiItem? = firstOrNull { it.lemma == lemma }

/** Лексема-кандидат по лемме карточки и ключу. */
fun List<LemmaCardUiItem>.lexemeOf(
    lemma: String,
    key: String,
): CardLexemeUiItem? = cardOf(lemma)?.lexemes?.firstOrNull { it.key == key }

/** Значение компонента у лексемы-кандидата по адресу компонента. */
fun List<LemmaCardUiItem>.valueOf(ref: ComponentRefUi): CandidateValueUiItem? =
    lexemeOf(ref.lemma, ref.key)?.values?.firstOrNull { it.componentId == ref.componentId }

/** Текст по адресу значения; null — значения нет или это «Выбор». */
fun List<LemmaCardUiItem>.textOf(ref: ValueRefUi): EditableTextUiItem? =
    (valueOf(ref.component) as? CandidateValueUiItem.Text)?.texts?.getOrNull(ref.index)

/** Адрес компонента из адреса значения. */
val ValueRefUi.component: ComponentRefUi
    get() = ComponentRefUi(lemma = lemma, key = key, componentId = componentId)

/** Карточка с изменением; остальные как были. */
fun List<LemmaCardUiItem>.updateCard(
    lemma: String,
    change: (LemmaCardUiItem) -> LemmaCardUiItem,
): List<LemmaCardUiItem> = map { if (it.lemma == lemma) change(it) else it }

/** Лексема-кандидат с изменением; остальные как были. */
fun List<LemmaCardUiItem>.updateLexeme(
    lemma: String,
    key: String,
    change: (CardLexemeUiItem) -> CardLexemeUiItem,
): List<LemmaCardUiItem> = updateCard(lemma) { card ->
    card.copy(lexemes = card.lexemes.map { if (it.key == key) change(it) else it })
}

/** Значение компонента с изменением; остальные как были. */
private fun List<LemmaCardUiItem>.updateValue(
    ref: ComponentRefUi,
    change: (CandidateValueUiItem) -> CandidateValueUiItem?,
): List<LemmaCardUiItem> = updateLexeme(ref.lemma, ref.key) { lexeme ->
    lexeme.copy(values = lexeme.values.mapNotNull { if (it.componentId == ref.componentId) change(it) else it })
}

/** Без карточки — лемма со всеми лексемами-кандидатами (AWP-15); null — карточки нет. */
fun List<LemmaCardUiItem>.deletingCard(lemma: String): List<LemmaCardUiItem>? {
    cardOf(lemma) ?: return null
    return filterNot { it.lemma == lemma }
}

/** Без лексемы-кандидата со всеми её значениями (AWP-15); null — её нет. */
fun List<LemmaCardUiItem>.deletingLexeme(
    lemma: String,
    key: String,
): List<LemmaCardUiItem>? {
    lexemeOf(lemma, key) ?: return null
    return updateCard(lemma) { card -> card.copy(lexemes = card.lexemes.filterNot { it.key == key }) }
}

/**
 * Без одного значения компонента (AWP-15.2, 15.7): у текста — текст по
 * номеру, у «Выбора» — опция по номеру; компонент без значений уходит;
 * ушёл последний текст последнего ядра — без ядра нет лексемы, она
 * удалена целиком (AWP-11.5); null — значения нет.
 */
fun List<LemmaCardUiItem>.deletingValue(
    ref: ValueRefUi,
    form: DictionaryFormUi,
): List<LemmaCardUiItem>? {
    val value = valueOf(ref.component) ?: return null
    return when (value) {
        is CandidateValueUiItem.Text -> {
            if (ref.index !in value.texts.indices) return null
            val kept = value.texts.filterIndexed { index, _ -> index != ref.index }
            withRemainingTexts(ref.component, value.copy(texts = kept), form)
        }
        is CandidateValueUiItem.Choice -> {
            if (ref.index !in value.options.indices) return null
            updateValue(ref.component) { value.copy(options = value.options.filterIndexed { index, _ -> index != ref.index }) }
        }
    }
}

/** Правка текста с обрезанными краями (AWP-16); null — значения нет. */
fun List<LemmaCardUiItem>.withTextEdit(
    ref: ValueRefUi,
    text: String,
): List<LemmaCardUiItem>? {
    textOf(ref) ?: return null
    return updateValue(ref.component) { value ->
        (value as CandidateValueUiItem.Text).copy(
            texts = value.texts.mapIndexed { index, item -> if (index == ref.index) item.copy(text = text.trim()) else item },
        )
    }
}

/**
 * Без стёртых текстов (AWP-16.5): пустой текст — удалённый, без текстов
 * уходит само значение; стёрт последний текст последнего ядра — уходит вся
 * лексема-кандидат (AWP-11.5); null — значения нет или пустых нет.
 */
fun List<LemmaCardUiItem>.droppingEmptyTexts(
    ref: ValueRefUi,
    form: DictionaryFormUi,
): List<LemmaCardUiItem>? {
    val value = valueOf(ref.component) as? CandidateValueUiItem.Text ?: return null
    val kept = value.texts.filter { it.text.isNotBlank() }
    if (kept.size == value.texts.size) return null
    return withRemainingTexts(ref.component, value.copy(texts = kept), form)
}

/**
 * Текстовое значение после удаления текстов: с текстами — как есть; без
 * текстов значение уходит, а если это было последнее ядро с текстом — без
 * ядра нет лексемы, уходит вся лексема-кандидат (AWP-11.5).
 */
private fun List<LemmaCardUiItem>.withRemainingTexts(
    ref: ComponentRefUi,
    value: CandidateValueUiItem.Text,
    form: DictionaryFormUi,
): List<LemmaCardUiItem> {
    if (value.texts.any { it.text.isNotBlank() }) return updateValue(ref) { value }
    val lexeme = lexemeOf(ref.lemma, ref.key) ?: return this
    val isCore = form.componentOf(ref.componentId)?.isCore == true
    val otherCoreHasText = lexeme.values.any { other ->
        other.componentId != ref.componentId &&
            other.hasContent &&
            form.componentOf(other.componentId)?.isCore == true
    }
    return if (isCore && !otherCoreHasText) {
        deletingLexeme(ref.lemma, ref.key) ?: this
    } else {
        updateValue(ref) { null }
    }
}

/** Текст ответа обратно (AWP-16.6); null — значения нет. */
fun List<LemmaCardUiItem>.withTextReset(ref: ValueRefUi): List<LemmaCardUiItem>? {
    textOf(ref) ?: return null
    return updateValue(ref.component) { value ->
        (value as CandidateValueUiItem.Text).copy(
            texts = value.texts.mapIndexed { index, item -> if (index == ref.index) item.copy(text = item.original) else item },
        )
    }
}

/**
 * Опция «Выбора» у одной лексемы-кандидата (AWP-17): у компонента с одним
 * значением — замена, null — «без значения»; у «несколько» — опция
 * добавляется, повтор снимает, null — все прочь; null в ответе — значения
 * или компонента нет.
 */
fun List<LemmaCardUiItem>.withOption(
    ref: ComponentRefUi,
    option: Long?,
    form: DictionaryFormUi,
): List<LemmaCardUiItem>? {
    val value = valueOf(ref) as? CandidateValueUiItem.Choice ?: return null
    val component = form.componentOf(ref.componentId) ?: return null
    val options = when {
        option == null -> emptyList()
        !component.isMultiple -> listOf(option)
        option in value.options -> value.options - option
        else -> value.options + option
    }
    return updateValue(ref) { value.copy(options = options) }
}

/** «Куда» лексемы-кандидата (AWP-18); null — её нет. */
fun List<LemmaCardUiItem>.withLexemeTarget(
    lemma: String,
    key: String,
    target: CardTargetUi,
): List<LemmaCardUiItem>? {
    lexemeOf(lemma, key) ?: return null
    return updateLexeme(lemma, key) { it.copy(target = target) }
}

/** Своя группа лексемы-кандидата при «новое слово» (AWP-08); null — её нет. */
fun List<LemmaCardUiItem>.withLexemeGroup(
    lemma: String,
    key: String,
    group: CardOwnGroupUi,
): List<LemmaCardUiItem>? {
    lexemeOf(lemma, key) ?: return null
    return updateLexeme(lemma, key) { it.copy(ownGroup = group) }
}

/**
 * После «Создать» (AWP-63.1, 63.3): у лексем-кандидатов карточки с «новое
 * слово» — созданное слово; явно выбранное слово не трогается; null —
 * карточки нет или таких лексем нет.
 */
fun List<LemmaCardUiItem>.withNewWordRetargeted(
    lemma: String,
    wordId: Long,
): List<LemmaCardUiItem>? {
    val card = cardOf(lemma) ?: return null
    if (card.lexemes.none { it.target == CardTargetUi.NewWord }) return null
    val target = CardTargetUi.Existing(wordId)
    return updateCard(lemma) { current ->
        current.copy(lexemes = current.lexemes.map { lexeme -> lexeme.retargetedFromNewWord(target) })
    }
}

private fun CardLexemeUiItem.retargetedFromNewWord(target: CardTargetUi): CardLexemeUiItem =
    if (this.target == CardTargetUi.NewWord) copy(target = target) else this

/** Удалённая группа (AWP-21.3, 21.5): своя группа лексем-кандидатов с ней → «как у всех». */
fun List<LemmaCardUiItem>.withoutGroup(groupId: Long): List<LemmaCardUiItem> = mapLexemes { lexeme ->
    if (lexeme.ownGroup == CardOwnGroupUi.Group(groupId)) lexeme.copy(ownGroup = CardOwnGroupUi.AsCommon) else lexeme
}

/** Удалённая лемма словаря (AWP-21.4): «Куда» лексем-кандидатов с ней → «новое слово». */
fun List<LemmaCardUiItem>.withoutTarget(wordId: Long): List<LemmaCardUiItem> = mapLexemes { lexeme ->
    if (lexeme.target == CardTargetUi.Existing(wordId)) lexeme.copy(target = CardTargetUi.NewWord) else lexeme
}

/** Каждая лексема-кандидат каждой карточки с изменением. */
private fun List<LemmaCardUiItem>.mapLexemes(change: (CardLexemeUiItem) -> CardLexemeUiItem): List<LemmaCardUiItem> =
    map { card -> card.copy(lexemes = card.lexemes.map(change)) }

/** Выключенный компонент (AWP-21.6): его значения уходят у всех лексем. */
fun List<LemmaCardUiItem>.withoutComponent(typeId: Long): List<LemmaCardUiItem> = map { card ->
    card.copy(lexemes = card.lexemes.map { lexeme -> lexeme.copy(values = lexeme.values.filterNot { it.componentId == typeId }) })
}
