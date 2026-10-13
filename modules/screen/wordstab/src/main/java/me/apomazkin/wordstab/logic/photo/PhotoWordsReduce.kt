package me.apomazkin.wordstab.logic.photo

import androidx.annotation.StringRes
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.PhotoDenyReason

/*
 * IS529: тексты отказов сервера глазами reducer'а (В5). Шаги диалога —
 * атомы PhotoStateAtoms, ветвление — в WordsTabReducer.
 */

/** Текст отказа по причине (WFT-63.1…63.4); ключ не задан — тот же текст, что 401 (WFT-19.1). */
@StringRes
internal fun errorResOf(reason: PhotoDenyReason): Int = when (reason) {
    PhotoDenyReason.NO_ACCESS_KEY -> R.string.photo_words_error_no_access
    PhotoDenyReason.UNAUTHORIZED -> R.string.photo_words_error_no_access
    PhotoDenyReason.RATE_LIMITED -> R.string.photo_words_error_rate_limited
    PhotoDenyReason.REQUEST_TOO_LARGE -> R.string.photo_words_error_too_many
    PhotoDenyReason.UNSUPPORTED_LANGUAGE -> R.string.photo_words_error_language
}
