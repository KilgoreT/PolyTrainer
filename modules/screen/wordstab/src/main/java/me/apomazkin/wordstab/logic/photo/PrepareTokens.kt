package me.apomazkin.wordstab.logic.photo

import me.apomazkin.textrecognition.RecognizedWord

/** Потолок запроса: сервер принимает не больше 2000 токенов (WFT-18, WFT-28). */
const val MAX_TOKENS = 2000

/**
 * Ш5: токены перед отправкой (WFT-17, WFT-18). Устройство убирает пустые
 * токены и повторы текста, оставляя первый по порядку с наибольшей
 * уверенностью, порядок первого появления сохраняет, больше ничего в
 * токенах не меняет и отправляет не больше [MAX_TOKENS] первых.
 * Нормализация (регистр, пунктуация, короткие) — на сервере (Ш8).
 */
fun prepareTokens(words: List<RecognizedWord>): List<RecognizedWord> {
    val tokens = dropBlankTokens(words)
    val unique = keepFirstOfDuplicates(tokens)
    return takeUpToLimit(unique)
}

/** Токен из пробелов или пустой — не токен (WFT-17.3). */
private fun dropBlankTokens(words: List<RecognizedWord>): List<RecognizedWord> =
    words.filter { it.text.isNotBlank() }

/**
 * Одинаковый текст — один токен на месте первого появления с наибольшей
 * уверенностью среди повторов (WFT-17.1, 17.4). Сравнение точное: регистр
 * и пунктуация различают токены, их снимает сервер.
 */
private fun keepFirstOfDuplicates(tokens: List<RecognizedWord>): List<RecognizedWord> {
    val firstByText = LinkedHashMap<String, RecognizedWord>()
    for (token in tokens) {
        val kept = firstByText[token.text]
        if (kept == null || token.confidence > kept.confidence) {
            firstByText[token.text] = kept?.copy(confidence = token.confidence) ?: token
        }
    }
    return firstByText.values.toList()
}

/** Не больше [MAX_TOKENS] первых (WFT-18). */
private fun takeUpToLimit(tokens: List<RecognizedWord>): List<RecognizedWord> =
    tokens.take(MAX_TOKENS)
