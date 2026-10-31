package me.apomazkin.lexeme

/*
 * IS537: леммы текста глазами приложения — домен, которым говорит порт
 * окна снимка после распознавания. Исход — типом на случай, редьюсер
 * исходы не разбирает. Порядок вариантов sealed — по важности.
 */

/**
 * Исход запроса лемм текста (LFT Ш1…Ш5): ответ со словарными формами или
 * «не разобрано», слов нет, отказ с причиной, сервер не ответил.
 */
sealed interface LemmasOutcome {
    /**
     * [dictionaryId] — словарь снимка (AWP-01), [lemmas] — текст, перевод и списки
     * как прислал сервер.
     */
    data class Found(
        val dictionaryId: Long,
        val lemmas: TextLemmas,
    ) : LemmasOutcome

    /** Ни словарной формы, ни «не разобрано» (TFP-19.2). */
    data object NoWords : LemmasOutcome

    /** [reason] — причина отказа, текст выбирает экран. */
    data class Denied(
        val reason: LookupDenyReason,
    ) : LemmasOutcome

    /** Сети нет или сервер не ответил. */
    data object Unavailable : LemmasOutcome
}

/**
 * Ответ сервера на токены снимка (LFT-14): [text] из токенов с
 * исправленными ошибками распознавания — контекст для запроса по слову,
 * [translation] на язык перевода, [lexical] — смысловые словарные формы в
 * порядке текста, [proper] — имена собственные, [function] — служебные
 * слова, [unparsed] — токены без пары и обрывки, как есть.
 */
data class TextLemmas(
    val text: String,
    val translation: String,
    val lexical: List<String>,
    val proper: List<String>,
    val function: List<String>,
    val unparsed: List<String>,
)
