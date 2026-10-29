package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.MateEffectHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import me.apomazkin.candidates.describe
import me.apomazkin.candidates.errorResOf
import me.apomazkin.candidates.mapper.toUi
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LemmaContext
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.scancandidates.LogTags
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase

/**
 * Исполнитель эффектов данных экрана: один эффект — один вызов юзкейса. Хендлер
 * ветвится по доменному исходу `when` прямо в ветке эффекта и шлёт сообщение на
 * каждый случай, данные внутри — через `toUi()`; решений он не принимает,
 * редьюсер исходов не разбирает. Ошибок не ловит: провал объявлен в эффекте
 * ([DatasourceEffect], `RecoverableEffect`).
 *
 * @param io диспатчер блокирующей работы; прод — `Dispatchers.IO`.
 */
class DatasourceEffectHandler(
    private val useCase: ScanCandidatesUseCase,
    private val logger: LexemeLogger,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, DatasourceEffect> {
    override val effectFamily = DatasourceEffect::class

    @Suppress("LongMethod")
    override suspend fun runEffect(
        effect: DatasourceEffect,
        consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.SCAN, message = "RunEffect: $effect")
        val msg = when (effect) {
            // Доступ к файлу галереи отпускается в любом исходе, и при сбое, и при
            // закрытии экрана посреди распознавания (Д52, TFP-22).
            is DatasourceEffect.RecognizeText -> withContext(io) {
                val outcome = try {
                    useCase.recognize(effect.uri)
                } finally {
                    withContext(NonCancellable) { useCase.releaseImageAccess(effect.uri) }
                }
                logger.d(tag = LogTags.SCAN, message = "recognize outcome=${outcome.describe()}")
                when (outcome) {
                    is RecognitionOutcome.Text -> Msg.TextRecognized(outcome.tokens)
                    RecognitionOutcome.Empty -> Msg.TextEmpty
                }
            }

            // Отказ по нейронке — свой текст с «Повторить» (AWP-53.1); остальные
            // отказы — текст по причине, без повтора (AWP-53.2–53.4, 53.8); сервер не
            // ответил — «Сервер не ответил» с «Повторить» (AWP-53.5).
            is DatasourceEffect.ExtractLemmas -> withContext(io) {
                val outcome = useCase.lemmasFromTokens(effect.tokens)
                logger.d(tag = LogTags.SCAN, message = "lemmas outcome=${outcome.describe()}")
                when (outcome) {
                    is LemmasOutcome.Found -> Msg.LemmasFound(
                        dictionaryId = outcome.dictionaryId,
                        text = outcome.lemmas.text,
                        translation = outcome.lemmas.translation,
                        lists = outcome.lemmas.toUi(),
                    )
                    LemmasOutcome.NoWords -> Msg.LemmasEmpty
                    is LemmasOutcome.Denied -> when (val reason = outcome.reason) {
                        LookupDenyReason.LLM_UNAVAILABLE ->
                            Msg.LemmasUnavailable(R.string.scan_analysis_unavailable)
                        LookupDenyReason.NO_ACCESS_KEY,
                        LookupDenyReason.UNAUTHORIZED,
                        LookupDenyReason.RATE_LIMITED,
                        LookupDenyReason.REQUEST_TOO_LARGE,
                        LookupDenyReason.UNSUPPORTED_LANGUAGE,
                        -> Msg.LemmasDenied(errorResOf(reason))
                    }
                    LemmasOutcome.Unavailable ->
                        Msg.LemmasUnavailable(R.string.photo_words_error_server)
                }
            }

            is DatasourceEffect.LoadForm -> withContext(io) {
                val outcome = useCase.loadForm(effect.dictionaryId, effect.lemmaTexts)
                logger.d(tag = LogTags.SCAN, message = "form outcome=${outcome.describe()}")
                when (outcome) {
                    is FormOutcome.Loaded -> Msg.FormLoaded(outcome.form.toUi())
                    FormOutcome.DictionaryMissing ->
                        Msg.FormFailed(R.string.photo_add_dictionary_missing)
                    is FormOutcome.Failed -> Msg.FormFailed(R.string.photo_add_form_failed)
                }
            }

            // Значения леммы с контекстом снимка у словаря снимка (LFL-103): не найдено
            // и «значений нет» — свой текст без повтора (AWP-55); сервер не ответил или
            // по слову не справился — «Повторить» (AWP-61.2, 61.5).
            is DatasourceEffect.LookupLemma -> withContext(io) {
                val outcome = useCase.lexemesFromLemma(
                    dictionaryId = effect.dictionaryId,
                    lemma = effect.lemma,
                    context = LemmaContext(text = effect.text, translation = effect.translation),
                )
                logger.d(tag = LogTags.SCAN, message = "lexemes word=${effect.word} outcome=${outcome.describe()}")
                when (outcome) {
                    is LexemesOutcome.Found -> Msg.LexemesFound(
                        text = effect.word,
                        word = outcome.word.toUi(),
                        recommendations = outcome.recommendations.map { it.toUi() },
                        existing = outcome.existing.map { it.toUi() },
                    )
                    LexemesOutcome.NotFound -> Msg.LexemesNotFound(effect.word, R.string.scan_not_found)
                    LexemesOutcome.NoLexemes -> Msg.LexemesNotFound(effect.word, R.string.scan_no_lexemes)
                    is LexemesOutcome.Denied -> deniedMsg(effect.word, outcome)
                    LexemesOutcome.Unavailable ->
                        Msg.LexemesUnavailable(effect.word, R.string.photo_words_error_server)
                    LexemesOutcome.WordFailed ->
                        Msg.LexemesUnavailable(effect.word, R.string.scan_server_unavailable)
                }
            }

            // Запись одного слова одной транзакцией (AWP-20): группа, слово и компонент
            // — своё сообщение на причину (AWP-21.3–21.6); прочие отказы — как есть.
            is DatasourceEffect.AddWord -> withContext(io) {
                val outcome = useCase.addWord(effect.batch)
                logger.d(tag = LogTags.SCAN, message = "add word=${effect.word} outcome=${outcome.describe()}")
                when (outcome) {
                    is AddWordsBatchOutcome.Added -> Msg.WordAdded(effect.word)
                    is AddWordsBatchOutcome.Rejected.GroupMissing ->
                        Msg.AddGroupGone(effect.word, outcome.toUi())
                    is AddWordsBatchOutcome.Rejected.WordMissing ->
                        Msg.AddWordGone(effect.word, outcome.toUi())
                    is AddWordsBatchOutcome.Rejected.TypeUnavailable ->
                        Msg.AddComponentGone(effect.word, outcome.toUi())
                    is AddWordsBatchOutcome.Rejected.NoCoreValue,
                    is AddWordsBatchOutcome.Rejected.DictionaryMissing,
                    -> Msg.AddFailed(effect.word, outcome.toUi())
                }
            }
        }
        consumer(msg)
    }
}

/**
 * Отказ на слове: по нейронке — «Сервер сейчас недоступен, попробуй позже» с
 * «Повторить» (AWP-61.1); остальные — текст по причине без повтора (AWP-61.3,
 * 61.4, 61.6, 61.8).
 */
private fun deniedMsg(
    word: String,
    outcome: LexemesOutcome.Denied,
): Msg = when (outcome.reason) {
    LookupDenyReason.LLM_UNAVAILABLE ->
        Msg.LexemesUnavailable(word, R.string.scan_server_unavailable)
    LookupDenyReason.NO_ACCESS_KEY,
    LookupDenyReason.UNAUTHORIZED,
    LookupDenyReason.RATE_LIMITED,
    LookupDenyReason.REQUEST_TOO_LARGE,
    LookupDenyReason.UNSUPPORTED_LANGUAGE,
    -> Msg.LexemesDenied(word, errorResOf(outcome.reason))
}

/** Строка исхода распознавания для лога. */
private fun RecognitionOutcome.describe(): String = when (this) {
    is RecognitionOutcome.Text -> "text tokens=${tokens.size}"
    RecognitionOutcome.Empty -> "empty"
}

/**
 * Строка исхода лемм для лога: словарь и число слов в каждом списке, `no words`,
 * `denied=…`, `unavailable`.
 */
private fun LemmasOutcome.describe(): String = when (this) {
    is LemmasOutcome.Found ->
        "dict=$dictionaryId lexical=${lemmas.lexical.size} proper=${lemmas.proper.size} " +
            "function=${lemmas.function.size} unparsed=${lemmas.unparsed.size}"
    LemmasOutcome.NoWords -> "no words"
    is LemmasOutcome.Denied -> "denied=$reason"
    LemmasOutcome.Unavailable -> "unavailable"
}
