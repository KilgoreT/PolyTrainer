package me.apomazkin.candidates

import androidx.annotation.StringRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.WordLookupOutcome

/*
 * IS533/IS536: тексты отказов (WCL-11.2, AWP-61) и строки исходов для лога —
 * общие для окна снимка и диалога карточки.
 */

/** Текст отказа по причине (ACC-26, LIM-110, LFL-106, LFL-30, LFL-99); ключ не задан — тот же текст, что 401 (ACC-19.1). */
@StringRes
fun errorResOf(reason: LookupDenyReason): Int = when (reason) {
    LookupDenyReason.NO_ACCESS_KEY -> R.string.photo_words_error_no_access
    LookupDenyReason.UNAUTHORIZED -> R.string.photo_words_error_no_access
    LookupDenyReason.RATE_LIMITED -> R.string.photo_words_error_rate_limited
    LookupDenyReason.REQUEST_TOO_LARGE -> R.string.photo_words_error_too_many
    LookupDenyReason.UNSUPPORTED_LANGUAGE -> R.string.photo_words_error_language
    LookupDenyReason.LLM_UNAVAILABLE -> R.string.scan_server_unavailable
}

/**
 * Строка исхода запроса по лемме для лога: `found lemmas=… recommendations=…
 * existing=…`, `not found`, `no lexemes`, `denied=…`, `unavailable`, `word failed`.
 */
fun LexemesOutcome.describe(): String = when (this) {
    is LexemesOutcome.Found ->
        "found lemmas=${word.lemmas.size} recommendations=${recommendations.size} existing=${existing.size}"
    LexemesOutcome.NotFound -> "not found"
    LexemesOutcome.NoLexemes -> "no lexemes"
    is LexemesOutcome.Denied -> "denied=$reason"
    LexemesOutcome.Unavailable -> "unavailable"
    LexemesOutcome.WordFailed -> "word failed"
}

/** Строка исхода запроса по слову карточки для лога. */
fun WordLookupOutcome.describe(): String = when (this) {
    is WordLookupOutcome.Found -> "found lemmas=${word.lemmas.size} recommendations=${recommendations.size}"
    WordLookupOutcome.NotFound -> "not found"
    WordLookupOutcome.WordMissing -> "word missing"
    is WordLookupOutcome.Denied -> "denied=$reason"
    WordLookupOutcome.Unavailable -> "unavailable"
    WordLookupOutcome.WordFailed -> "word failed"
}

/** Строка исхода формы для лога. */
fun FormOutcome.describe(): String = when (this) {
    is FormOutcome.Loaded ->
        "loaded components=${form.components.size} options=${form.options.values.sumOf { it.size }} groups=${form.groups.size} existing=${form.existingLemmas.size}"
    FormOutcome.DictionaryMissing -> "dictionary missing"
    is FormOutcome.Failed -> "failed: $cause"
}

/** Строка исхода записи для лога. */
fun AddWordsBatchOutcome.describe(): String = when (this) {
    is AddWordsBatchOutcome.Added -> "added words=${wordIds.size} lexemes=${lexemeIds.size}"
    is AddWordsBatchOutcome.Rejected -> "rejected $this"
}
