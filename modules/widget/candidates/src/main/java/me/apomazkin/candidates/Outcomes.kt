package me.apomazkin.candidates

import androidx.annotation.StringRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.WordLookupOutcome

/*
 * IS533/IS536: тексты отказов (WFT-63) и строки исходов для лога — общие
 * для окна снимка и диалога карточки.
 */

/** Текст отказа по причине (WFT-63.1…63.5); ключ не задан — тот же текст, что 401 (WFT-19.1). */
@StringRes
fun errorResOf(reason: LookupDenyReason): Int = when (reason) {
    LookupDenyReason.NO_ACCESS_KEY -> R.string.photo_words_error_no_access
    LookupDenyReason.UNAUTHORIZED -> R.string.photo_words_error_no_access
    LookupDenyReason.RATE_LIMITED -> R.string.photo_words_error_rate_limited
    LookupDenyReason.REQUEST_TOO_LARGE -> R.string.photo_words_error_too_many
    LookupDenyReason.UNSUPPORTED_LANGUAGE -> R.string.photo_words_error_language
    LookupDenyReason.LLM_UNAVAILABLE -> R.string.scan_server_unavailable
}

/** Строка исхода запроса для лога: `dict=… words=… lemmas=… recommendations=…`, `no words`, `denied=…`, `unavailable`. */
fun LookupOutcome.describe(): String = when (this) {
    is LookupOutcome.Words ->
        "dict=$dictionaryId words=${words.size} lemmas=${words.sumOf { it.lemmas.size }} recommendations=${recommendations.size}"
    LookupOutcome.NoWords -> "no words"
    is LookupOutcome.Denied -> "denied=$reason"
    LookupOutcome.Unavailable -> "unavailable"
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
