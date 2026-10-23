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
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupOutcome
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

    override suspend fun runEffect(
        effect: DatasourceEffect,
        consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.SCAN, message = "RunEffect: $effect")
        val msg = when (effect) {
            // Доступ к файлу галереи отпускается в любом исходе, и при сбое, и при
            // закрытии экрана посреди распознавания (Д52, WFP-22).
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

            // Недоступная нейронка — «Сервер сейчас недоступен» с повтором (WFP-26.2),
            // остальные отказы — только «Закрыть» (WFP-26.3).
            is DatasourceEffect.LookupWords -> withContext(io) {
                val outcome = useCase.lookupWords(effect.tokens)
                logger.d(tag = LogTags.SCAN, message = "lookup outcome=${outcome.describe()}")
                when (outcome) {
                    is LookupOutcome.Words -> Msg.WordsFound(
                        dictionaryId = outcome.dictionaryId,
                        words = outcome.words.map { it.toUi() },
                        recommendations = outcome.recommendations.map { it.toUi() },
                    )
                    LookupOutcome.NoWords -> Msg.WordsEmpty
                    is LookupOutcome.Denied -> when (val reason = outcome.reason) {
                        LookupDenyReason.LLM_UNAVAILABLE ->
                            Msg.LookupUnavailable(R.string.scan_server_unavailable)
                        LookupDenyReason.NO_ACCESS_KEY,
                        LookupDenyReason.UNAUTHORIZED,
                        LookupDenyReason.RATE_LIMITED,
                        LookupDenyReason.REQUEST_TOO_LARGE,
                        LookupDenyReason.UNSUPPORTED_LANGUAGE,
                        -> Msg.LookupDenied(errorResOf(reason))
                    }
                    LookupOutcome.Unavailable ->
                        Msg.LookupUnavailable(R.string.photo_words_error_server)
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

            is DatasourceEffect.AddWords -> withContext(io) {
                val outcome = useCase.addWords(effect.batch)
                logger.d(tag = LogTags.SCAN, message = "add outcome=${outcome.describe()}")
                when (outcome) {
                    is AddWordsBatchOutcome.Added -> Msg.AddFinished(
                        lexemeCount = outcome.lexemeIds.size,
                        wordCount = outcome.wordIds.size,
                    )
                    is AddWordsBatchOutcome.Rejected.GroupMissing ->
                        Msg.AddGroupGone(outcome.toUi())
                    is AddWordsBatchOutcome.Rejected.WordMissing ->
                        Msg.AddWordGone(outcome.toUi())
                    is AddWordsBatchOutcome.Rejected.TypeUnavailable ->
                        Msg.AddComponentGone(outcome.toUi())
                    is AddWordsBatchOutcome.Rejected.NoCoreValue,
                    is AddWordsBatchOutcome.Rejected.DictionaryMissing,
                    -> Msg.AddFailed(outcome.toUi())
                }
            }
        }
        consumer(msg)
    }
}

/** Строка исхода распознавания для лога. */
private fun RecognitionOutcome.describe(): String = when (this) {
    is RecognitionOutcome.Text -> "text tokens=${tokens.size}"
    RecognitionOutcome.Empty -> "empty"
}
