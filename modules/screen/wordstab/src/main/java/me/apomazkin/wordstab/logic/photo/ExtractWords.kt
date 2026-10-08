package me.apomazkin.wordstab.logic.photo

import me.apomazkin.textrecognition.RecognizedWord

/**
 * Пороги отсева. По умолчанию выключены: включаются после первых
 * ручников, когда по логам видны реальные значения уверенности.
 * [language] — код языка словаря; слово без определённого языка (null у
 * ML Kit) при включённом фильтре не отсеивается.
 */
data class WordRules(
    val minLength: Int = 2,
    val minConfidence: Float = 0f,
    val language: String? = null,
)

/**
 * Слова ML Kit → список для показа: по краям снято всё, что не буква и
 * не цифра (пунктуация, кавычки, перенос `well-`, притяжательный `dogs'`),
 * апостроф и дефис внутри остаются сами (`don't`, `well-known`), токены
 * без букв и короче [WordRules.minLength] выброшены, регистр нижний,
 * дубли схлопнуты, порядок — первое появление (Д4 брифа, Д10 IS523).
 */
fun extractWords(
    words: List<RecognizedWord>,
    rules: WordRules = WordRules(),
): List<String> = words
    .asSequence()
    .filter { it.confidence >= rules.minConfidence }
    .filter { rules.language == null || it.language == null || it.language == rules.language }
    .map { it.text.trim { char -> !char.isLetterOrDigit() }.lowercase() }
    .filter { it.length >= rules.minLength && it.any(Char::isLetter) }
    .distinct()
    .toList()
