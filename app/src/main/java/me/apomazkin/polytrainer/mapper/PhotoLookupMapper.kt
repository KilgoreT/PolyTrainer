package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.CandidateLexeme
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.Lemma
import me.apomazkin.core_network_api.entity.LookupRequest
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.TokenQuery
import me.apomazkin.core_network_api.entity.WordResult
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.deps.PhotoDenyReason
import me.apomazkin.wordstab.deps.PhotoLemma
import me.apomazkin.wordstab.deps.PhotoLexeme
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.deps.PhotoWordStatus

/*
 * IS529/IS532: граница «слой сети → экран». Сущности сети в экран не
 * ходят, экран получает PhotoLookupOutcome со словарём снимка (AWP-01) и
 * леммами ответа с лексемами-кандидатами (WFT-76, AWP-04).
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

/** Исход слоя сети → исход для экрана (WFT-50, WFT-63); [dictionaryId] — словарь снимка. */
fun LookupResult.toOutcome(dictionaryId: Long): PhotoLookupOutcome = when (this) {
    is LookupResult.Success -> PhotoLookupOutcome.Words(dictionaryId = dictionaryId, words = words.map { it.toPhotoWord() })
    is LookupResult.Denied -> PhotoLookupOutcome.Denied(reason.toPhotoDenyReason())
    is LookupResult.Unavailable -> PhotoLookupOutcome.Unavailable
}

/** Слово со статусом и леммами ответа; у «не найдено» и «ошибка источника» лемм нет (WFT-50). */
private fun WordResult.toPhotoWord(): PhotoWord = PhotoWord(
    text = text,
    status = status.toPhotoWordStatus(),
    lemmas = lemmas.map { it.toPhotoLemma() },
)

private fun Lemma.toPhotoLemma(): PhotoLemma = PhotoLemma(
    text = text,
    formNote = formNote,
    lexemes = lexemes.map { it.toPhotoLexeme() },
)

private fun CandidateLexeme.toPhotoLexeme(): PhotoLexeme = PhotoLexeme(
    key = key,
    partOfSpeechKey = partOfSpeechKey,
    source = source,
    translations = translations,
    gloss = gloss,
    caption = caption,
    notes = notes,
    examples = examples,
    parent = parent,
    parentKey = parentKey,
)

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
