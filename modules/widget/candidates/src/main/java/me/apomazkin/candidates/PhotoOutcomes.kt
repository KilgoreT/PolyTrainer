package me.apomazkin.candidates

import androidx.annotation.StringRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome

/*
 * IS533: исходы запроса, формы и записи глазами экранов — тексты отказов
 * (WFT-63) и строки для лога; общие для окна снимка и диалога карточки.
 */

/** Текст отказа по причине (WFT-63.1…63.4); ключ не задан — тот же текст, что 401 (WFT-19.1). */
@StringRes
fun errorResOf(reason: PhotoDenyReason): Int = when (reason) {
    PhotoDenyReason.NO_ACCESS_KEY -> R.string.photo_words_error_no_access
    PhotoDenyReason.UNAUTHORIZED -> R.string.photo_words_error_no_access
    PhotoDenyReason.RATE_LIMITED -> R.string.photo_words_error_rate_limited
    PhotoDenyReason.REQUEST_TOO_LARGE -> R.string.photo_words_error_too_many
    PhotoDenyReason.UNSUPPORTED_LANGUAGE -> R.string.photo_words_error_language
}

/** Отказ записи до первой вставки → причина для экрана (AWP-21, WCL-13). */
fun AddWordsBatchOutcome.Rejected.toFailure(): PhotoAddFailure = when (this) {
    is AddWordsBatchOutcome.Rejected.GroupMissing -> PhotoAddFailure.GroupMissing(groupId)
    is AddWordsBatchOutcome.Rejected.WordMissing -> PhotoAddFailure.WordMissing(wordId)
    is AddWordsBatchOutcome.Rejected.TypeUnavailable -> PhotoAddFailure.TypeUnavailable(typeId)
    is AddWordsBatchOutcome.Rejected.NoCoreValue -> PhotoAddFailure.NoCoreValue
    AddWordsBatchOutcome.Rejected.DictionaryMissing -> PhotoAddFailure.DictionaryMissing
}

/** Строка исхода запроса для лога: `dict=… words=… lemmas=…`, `denied=…`, `unavailable`. */
fun PhotoLookupOutcome.describe(): String = when (this) {
    is PhotoLookupOutcome.Words -> "dict=$dictionaryId words=${words.size} lemmas=${words.sumOf { it.lemmas.size }}"
    is PhotoLookupOutcome.Denied -> "denied=$reason"
    PhotoLookupOutcome.Unavailable -> "unavailable"
}

/** Строка исхода формы для лога. */
fun PhotoFormOutcome.describe(): String = when (this) {
    is PhotoFormOutcome.Loaded ->
        "loaded components=${form.components.size} options=${form.partOfSpeechOptions.size} groups=${form.groups.size} existing=${form.existingLemmas.size} remembered=${remembered != null}"
    PhotoFormOutcome.DictionaryMissing -> "dictionary missing"
    is PhotoFormOutcome.Failed -> "failed: $cause"
}

/** Строка исхода записи для лога. */
fun AddWordsBatchOutcome.describe(): String = when (this) {
    is AddWordsBatchOutcome.Added -> "added words=${wordIds.size} lexemes=${lexemeIds.size}"
    is AddWordsBatchOutcome.Rejected -> "rejected $this"
}
