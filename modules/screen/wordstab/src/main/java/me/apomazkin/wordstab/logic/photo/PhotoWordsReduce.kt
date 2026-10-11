package me.apomazkin.wordstab.logic.photo

import androidx.annotation.StringRes
import io.github.kilgoret.mate.Effect
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.PhotoDenyReason
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.showNoPhotoWords
import me.apomazkin.wordstab.logic.showPhotoError
import me.apomazkin.wordstab.logic.showPhotoWords
import me.apomazkin.wordstab.logic.startLooking

/*
 * IS529: шаги Ш5 → Ш6 и Ш10 → Ш11 / В2 / В3 / В5 глазами редьюсера.
 * Проверка «свой ли запуск» остаётся в WordsTabReducer (WFT-62).
 */

/** Токены готовы: пусто → «слов не найдено» без запроса (WFT-60.1), иначе → на сервер (WFT-16.2). */
internal fun WordsTabState.acceptTokens(
        tokens: List<RecognizedWord>,
): Pair<WordsTabState, Set<Effect>> {
    if (tokens.isEmpty()) return showNoPhotoWords() to emptySet()
    val looking = startLooking()
    return looking to setOf(
            DatasourceEffect.LookupWords(
                    tokens = tokens,
                    requestId = looking.photoWordsState.requestId,
            ),
    )
}

/** Исход запроса: слова (WFT-56), пусто (WFT-60.2), отказ с текстом по причине (WFT-63), «сервер не ответил» (WFT-23). */
internal fun WordsTabState.acceptLookupOutcome(
        outcome: PhotoLookupOutcome,
): WordsTabState = when (outcome) {
    is PhotoLookupOutcome.Words -> if (outcome.words.isEmpty()) showNoPhotoWords() else showPhotoWords(outcome.words)
    is PhotoLookupOutcome.Denied -> showPhotoError(errorResOf(outcome.reason))
    PhotoLookupOutcome.Unavailable -> showPhotoError(R.string.photo_words_error_server)
}

/** Текст отказа по причине (WFT-63.1…63.4); ключ не задан — тот же текст, что 401 (WFT-19.1). */
@StringRes
internal fun errorResOf(reason: PhotoDenyReason): Int = when (reason) {
    PhotoDenyReason.NO_ACCESS_KEY -> R.string.photo_words_error_no_access
    PhotoDenyReason.UNAUTHORIZED -> R.string.photo_words_error_no_access
    PhotoDenyReason.RATE_LIMITED -> R.string.photo_words_error_rate_limited
    PhotoDenyReason.REQUEST_TOO_LARGE -> R.string.photo_words_error_too_many
    PhotoDenyReason.UNSUPPORTED_LANGUAGE -> R.string.photo_words_error_language
}
