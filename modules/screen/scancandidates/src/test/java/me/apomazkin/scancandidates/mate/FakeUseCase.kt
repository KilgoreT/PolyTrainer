package me.apomazkin.scancandidates.mate

import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LemmaContext
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase

/**
 * Юзкейс экрана лямбдами: по умолчанию каждый вызов отдаёт «ничего нет».
 * [calls] хранит порядок вызовов строками `имя:аргумент`, чтобы тест видел, что
 * доступ к файлу отпущен после распознавания; [lexemeRequests] и [batches] —
 * аргументы запросов значений и записей.
 */
internal class FakeUseCase(
    var recognizeImpl: suspend (String) -> RecognitionOutcome = { RecognitionOutcome.Empty },
    var releaseImpl: suspend (String) -> Unit = {},
    var lemmasImpl: suspend (List<LookupToken>) -> LemmasOutcome = { LemmasOutcome.NoWords },
    var loadFormImpl: suspend (Long, List<String>) -> FormOutcome =
        { _, _ -> FormOutcome.DictionaryMissing },
    var lexemesImpl: suspend (Long, String, LemmaContext) -> LexemesOutcome =
        { _, _, _ -> LexemesOutcome.NotFound },
    var addWordImpl: suspend (WordsBatch) -> AddWordsBatchOutcome =
        { AddWordsBatchOutcome.Added(wordIds = emptyList(), lexemeIds = emptyList()) },
) : ScanCandidatesUseCase {
    val calls = mutableListOf<String>()
    val extracted = mutableListOf<List<LookupToken>>()
    val formRequests = mutableListOf<Pair<Long, List<String>>>()
    val lexemeRequests = mutableListOf<Triple<Long, String, LemmaContext>>()
    val batches = mutableListOf<WordsBatch>()

    override suspend fun recognize(uri: String): RecognitionOutcome {
        calls += "recognize:$uri"
        return recognizeImpl(uri)
    }

    override suspend fun releaseImageAccess(uri: String) {
        calls += "release:$uri"
        releaseImpl(uri)
    }

    override suspend fun lemmasFromTokens(tokens: List<LookupToken>): LemmasOutcome {
        calls += "lemmas:${tokens.size}"
        extracted += tokens
        return lemmasImpl(tokens)
    }

    override suspend fun loadForm(
        dictionaryId: Long,
        lemmaTexts: List<String>,
    ): FormOutcome {
        calls += "form:$dictionaryId"
        formRequests += dictionaryId to lemmaTexts
        return loadFormImpl(dictionaryId, lemmaTexts)
    }

    override suspend fun lexemesFromLemma(
        dictionaryId: Long,
        lemma: String,
        context: LemmaContext,
    ): LexemesOutcome {
        calls += "lexemes:$lemma"
        lexemeRequests += Triple(dictionaryId, lemma, context)
        return lexemesImpl(dictionaryId, lemma, context)
    }

    override suspend fun addWord(batch: WordsBatch): AddWordsBatchOutcome {
        calls += "add:${batch.words.joinToString(",") { it.text }}"
        batches += batch
        return addWordImpl(batch)
    }
}
