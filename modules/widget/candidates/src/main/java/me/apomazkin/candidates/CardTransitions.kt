package me.apomazkin.candidates

/*
 * IS533: переходы над списком карточек — чистые функции, общие для окна
 * снимка (wordstab) и диалога в карточке слова (wordcard). Атомы экранов
 * оборачивают их логом и эффектами; null — «шага нет», атом пишет no-op.
 */

/** Карточка по тексту леммы. */
fun List<PhotoCard>.cardOf(lemma: String): PhotoCard? = firstOrNull { it.lemma == lemma }

/** Лексема-кандидат по лемме карточки и ключу. */
fun List<PhotoCard>.lexemeOf(
    lemma: String,
    key: String,
): PhotoCardLexeme? = cardOf(lemma)?.lexemes?.firstOrNull { it.key == key }

/** Карточка с изменением; остальные как были. */
fun List<PhotoCard>.updateCard(
    lemma: String,
    change: (PhotoCard) -> PhotoCard,
): List<PhotoCard> = map { if (it.lemma == lemma) change(it) else it }

/** Лексема-кандидат с изменением; остальные как были. */
fun List<PhotoCard>.updateLexeme(
    lemma: String,
    key: String,
    change: (PhotoCardLexeme) -> PhotoCardLexeme,
): List<PhotoCard> = updateCard(lemma) { card ->
    card.copy(lexemes = card.lexemes.map { if (it.key == key) change(it) else it })
}

/** Развернуть или свернуть карточку; null — карточки нет. */
fun List<PhotoCard>.togglingCard(lemma: String): List<PhotoCard>? {
    cardOf(lemma) ?: return null
    return updateCard(lemma) { it.copy(isExpanded = !it.isExpanded) }
}

/** Без карточки — лемма со всеми лексемами-кандидатами (AWP-15); null — карточки нет. */
fun List<PhotoCard>.deletingCard(lemma: String): List<PhotoCard>? {
    cardOf(lemma) ?: return null
    return filterNot { it.lemma == lemma }
}

/** Без лексемы-кандидата с её примерами (AWP-15.1); «ещё N» не меняется (AWP-05.4); null — её нет. */
fun List<PhotoCard>.deletingLexeme(
    lemma: String,
    key: String,
): List<PhotoCard>? {
    lexemeOf(lemma, key) ?: return null
    return updateCard(lemma) { card -> card.copy(lexemes = card.lexemes.filterNot { it.key == key }) }
}

/** Без одного примера лексемы-кандидата (AWP-15.2); null — примера нет. */
fun List<PhotoCard>.deletingExample(
    lemma: String,
    key: String,
    index: Int,
): List<PhotoCard>? {
    val lexeme = lexemeOf(lemma, key) ?: return null
    if (index !in lexeme.examples.indices) return null
    return updateLexeme(lemma, key) { it.copy(examples = it.examples.filterIndexed { i, _ -> i != index }) }
}

/** Правка текста с обрезанными краями (AWP-16); null — лексемы или поля нет. */
fun List<PhotoCard>.withTextEdit(
    lemma: String,
    key: String,
    field: PhotoTextField,
    text: String,
): List<PhotoCard>? {
    val lexeme = lexemeOf(lemma, key) ?: return null
    val edited = lexeme.withText(field, text.trim()) ?: return null
    return updateLexeme(lemma, key) { edited }
}

/** Без примеров со стёртым текстом (AWP-16.5); null — лексемы нет или пустых нет. */
fun List<PhotoCard>.droppingEmptyExamples(
    lemma: String,
    key: String,
): List<PhotoCard>? {
    val lexeme = lexemeOf(lemma, key) ?: return null
    val kept = lexeme.examples.filter { it.text.isNotBlank() }
    if (kept.size == lexeme.examples.size) return null
    return updateLexeme(lemma, key) { it.copy(examples = kept) }
}

/** Текст источника обратно (AWP-16.6); null — лексемы или поля нет. */
fun List<PhotoCard>.withTextReset(
    lemma: String,
    key: String,
    field: PhotoTextField,
): List<PhotoCard>? {
    val lexeme = lexemeOf(lemma, key) ?: return null
    val original = lexeme.originalOf(field) ?: return null
    val reset = lexeme.withText(field, original) ?: return null
    return updateLexeme(lemma, key) { reset }
}

/** Часть речи одной лексемы-кандидата (AWP-17); null — её нет. */
fun List<PhotoCard>.withPartOfSpeech(
    lemma: String,
    key: String,
    optionId: Long?,
): List<PhotoCard>? {
    lexemeOf(lemma, key) ?: return null
    return updateLexeme(lemma, key) { it.copy(partOfSpeechOptionId = optionId) }
}

/** «Куда класть» карточки (AWP-18); null — карточки нет. */
fun List<PhotoCard>.withCardTarget(
    lemma: String,
    target: PhotoCardTarget,
): List<PhotoCard>? {
    cardOf(lemma) ?: return null
    return updateCard(lemma) { it.copy(target = target) }
}

/** Своя группа карточки (AWP-08); null — карточки нет. */
fun List<PhotoCard>.withCardGroup(
    lemma: String,
    group: PhotoOwnGroup,
): List<PhotoCard>? {
    cardOf(lemma) ?: return null
    return updateCard(lemma) { it.copy(ownGroup = group) }
}

/** Удалённая группа (AWP-21.3, 21.5): своя группа карточек с ней → «как у всех». */
fun List<PhotoCard>.withoutGroup(groupId: Long): List<PhotoCard> = map { card ->
    if (card.ownGroup == PhotoOwnGroup.Group(groupId)) card.copy(ownGroup = PhotoOwnGroup.AsCommon) else card
}

/** Удалённая лемма словаря (AWP-21.4): «куда класть» карточек с ней → «новое слово». */
fun List<PhotoCard>.withoutTarget(wordId: Long): List<PhotoCard> = map { card ->
    if (card.target == PhotoCardTarget.Existing(wordId)) card.copy(target = PhotoCardTarget.NewWord) else card
}

/** Текст поля заменён; null — поля нет. */
fun PhotoCardLexeme.withText(
    field: PhotoTextField,
    text: String,
): PhotoCardLexeme? = when (field) {
    PhotoTextField.Translation -> copy(translation = translation.copy(text = text))
    PhotoTextField.Gloss -> gloss?.let { copy(gloss = it.copy(text = text)) }
    is PhotoTextField.Example -> examples.getOrNull(field.index)?.let { example ->
        copy(examples = examples.mapIndexed { i, item -> if (i == field.index) example.copy(text = text) else item })
    }
}

/** Текущий текст поля; null — поля нет. */
fun PhotoCardLexeme.textOf(field: PhotoTextField): String? = when (field) {
    PhotoTextField.Translation -> translation.text
    PhotoTextField.Gloss -> gloss?.text
    is PhotoTextField.Example -> examples.getOrNull(field.index)?.text
}

/** Текст источника поля; null — поля нет. */
fun PhotoCardLexeme.originalOf(field: PhotoTextField): String? = when (field) {
    PhotoTextField.Translation -> translation.original
    PhotoTextField.Gloss -> gloss?.original
    is PhotoTextField.Example -> examples.getOrNull(field.index)?.original
}
