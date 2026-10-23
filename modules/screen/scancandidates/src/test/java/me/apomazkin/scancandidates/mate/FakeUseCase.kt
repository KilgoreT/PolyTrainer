package me.apomazkin.scancandidates.mate

import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase

/**
 * Юзкейс экрана лямбдами: по умолчанию каждый вызов отдаёт «ничего нет».
 * [calls] хранит порядок вызовов строками `имя:аргумент`, чтобы тест видел, что
 * доступ к файлу отпущен после распознавания.
 */
internal class FakeUseCase(
    var recognizeImpl: suspend (String) -> RecognitionOutcome = { RecognitionOutcome.Empty },
    var releaseImpl: suspend (String) -> Unit = {},
    var lookupImpl: suspend (List<LookupToken>) -> LookupOutcome = { LookupOutcome.NoWords },
    var loadFormImpl: suspend (Long, List<String>) -> FormOutcome =
        { _, _ -> FormOutcome.DictionaryMissing },
    var addImpl: suspend (WordsBatch) -> AddWordsBatchOutcome =
        { AddWordsBatchOutcome.Rejected.DictionaryMissing },
) : ScanCandidatesUseCase {
    val calls = mutableListOf<String>()
    val lookedUp = mutableListOf<List<LookupToken>>()
    val formRequests = mutableListOf<Pair<Long, List<String>>>()
    val batches = mutableListOf<WordsBatch>()

    override suspend fun recognize(uri: String): RecognitionOutcome {
        calls += "recognize:$uri"
        return recognizeImpl(uri)
    }

    override suspend fun releaseImageAccess(uri: String) {
        calls += "release:$uri"
        releaseImpl(uri)
    }

    override suspend fun lookupWords(tokens: List<LookupToken>): LookupOutcome {
        calls += "lookup:${tokens.size}"
        lookedUp += tokens
        return lookupImpl(tokens)
    }

    override suspend fun loadForm(
        dictionaryId: Long,
        lemmaTexts: List<String>,
    ): FormOutcome {
        calls += "form:$dictionaryId"
        formRequests += dictionaryId to lemmaTexts
        return loadFormImpl(dictionaryId, lemmaTexts)
    }

    override suspend fun addWords(batch: WordsBatch): AddWordsBatchOutcome {
        calls += "add:${batch.words.size}"
        batches += batch
        return addImpl(batch)
    }
}
