package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LookupRequest
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.TokenQuery
import me.apomazkin.core_network_api.entity.WordResult
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.deps.PhotoDenyReason
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.deps.PhotoWordStatus

/*
 * IS529: граница «слой сети → экран». Сущности сети в экран не ходят,
 * экран получает PhotoLookupOutcome с тем, что показывает (Д4: первая
 * лемма и часть речи).
 */

/** Токены как распознаны плюс языки словаря → запрос `lookupWords` (WFT-20). */
fun lookupRequestOf(dictionary: DictionaryApiEntity, tokens: List<RecognizedWord>): LookupRequest =
    LookupRequest(
        learningLanguage = dictionary.learningLanguage,
        translationLanguage = dictionary.translationLanguage,
        tokens = tokens.map { it.toTokenQuery() },
    )

private fun RecognizedWord.toTokenQuery(): TokenQuery = TokenQuery(
    text = text,
    confidence = confidence,
    language = language,
)

/** Исход слоя сети → исход для экрана (WFT-56, WFT-63). */
fun LookupResult.toOutcome(): PhotoLookupOutcome = when (this) {
    is LookupResult.Success -> PhotoLookupOutcome.Words(words.map { it.toPhotoWord() })
    is LookupResult.Denied -> PhotoLookupOutcome.Denied(reason.toPhotoDenyReason())
    is LookupResult.Unavailable -> PhotoLookupOutcome.Unavailable
}

/**
 * Слово со статусом; у найденного — лемма, часть речи, первое значение на
 * языке перевода и первое толкование первого кандидата (Д4, Д5, Д6,
 * WFT-56.7…56.10). Чего нет у кандидата — null.
 */
private fun WordResult.toPhotoWord(): PhotoWord {
    val first = candidates.firstOrNull()
    return PhotoWord(
        text = text,
        status = status.toPhotoWordStatus(),
        lemma = first?.lemma,
        partOfSpeech = first?.partOfSpeech,
        meaning = first?.meanings?.firstOrNull(),
        gloss = first?.senses?.firstOrNull()?.gloss,
    )
}

private fun WordStatus.toPhotoWordStatus(): PhotoWordStatus = when (this) {
    WordStatus.FOUND -> PhotoWordStatus.FOUND
    WordStatus.NOT_FOUND -> PhotoWordStatus.NOT_FOUND
    WordStatus.ERROR -> PhotoWordStatus.ERROR
}

private fun DenyReason.toPhotoDenyReason(): PhotoDenyReason = when (this) {
    DenyReason.NO_ACCESS_KEY -> PhotoDenyReason.NO_ACCESS_KEY
    DenyReason.UNAUTHORIZED -> PhotoDenyReason.UNAUTHORIZED
    DenyReason.RATE_LIMITED -> PhotoDenyReason.RATE_LIMITED
    DenyReason.REQUEST_TOO_LARGE -> PhotoDenyReason.REQUEST_TOO_LARGE
    DenyReason.UNSUPPORTED_LANGUAGE -> PhotoDenyReason.UNSUPPORTED_LANGUAGE
}
