package me.apomazkin.core_network_impl.mapper

import me.apomazkin.core_network_api.entity.Candidate
import me.apomazkin.core_network_api.entity.LookupRequest
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.Sense
import me.apomazkin.core_network_api.entity.TokenQuery
import me.apomazkin.core_network_api.entity.WordResult
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.core_network_impl.dto.CandidateDto
import me.apomazkin.core_network_impl.dto.LookupRequestDto
import me.apomazkin.core_network_impl.dto.LookupResponseDto
import me.apomazkin.core_network_impl.dto.SenseDto
import me.apomazkin.core_network_impl.dto.TokenQueryDto
import me.apomazkin.core_network_impl.dto.WordResultDto

/* Маппинг сущностей api-модуля в формат провода и обратно, один в один. */

internal fun LookupRequest.toDto(): LookupRequestDto = LookupRequestDto(
        learningLanguage = learningLanguage,
        translationLanguage = translationLanguage,
        tokens = tokens.map { it.toDto() },
)

private fun TokenQuery.toDto(): TokenQueryDto = TokenQueryDto(
        text = text,
        confidence = confidence,
        language = language,
)

internal fun LookupResponseDto.toSuccess(): LookupResult.Success = LookupResult.Success(
        attribution = attribution,
        words = words.map { it.toEntity() },
)

private fun WordResultDto.toEntity(): WordResult = WordResult(
        text = text,
        status = statusOf(status),
        candidates = candidates.map { it.toEntity() },
)

/** Статус как в контракте; неизвестная строка — «ошибка источника», не падение. */
private fun statusOf(raw: String): WordStatus = when (raw) {
    "found" -> WordStatus.FOUND
    "not_found" -> WordStatus.NOT_FOUND
    else -> WordStatus.ERROR
}

private fun CandidateDto.toEntity(): Candidate = Candidate(
        lemma = lemma,
        partOfSpeech = partOfSpeech,
        formNote = formNote,
        senses = senses.map { it.toEntity() },
        meanings = meanings,
)

private fun SenseDto.toEntity(): Sense = Sense(
        gloss = gloss,
        examples = examples,
)
