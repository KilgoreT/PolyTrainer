package me.apomazkin.wordstab.logic

import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoFormOutcome
import me.apomazkin.candidates.describe
import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoMapping
import android.net.Uri
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.wordstab.logic.photo.prepareTokens
import me.apomazkin.mate.EMPTY_STRING
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.RecoverableEffect
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger

sealed interface DatasourceEffect : Effect {

    data class LoadTermFlow(
            val pattern: String = EMPTY_STRING,
    ) : DatasourceEffect

    data class CreateWord(val value: String) : DatasourceEffect
    data class UpdateWord(val wordId: Long, val value: String) : DatasourceEffect

    data class RemoveWords(val wordSet: Set<WordInfo>) : DatasourceEffect

    /**
     * IS527: распознать текст на снимке; [uri] строкой, см. Msg.PhotoPicked.
     * Провал (недекодируемая картинка, ML Kit, OOM) объявлен в типе:
     * хендлер ошибки не ловит, стектрейс пишет ErrorLoggingObserver.
     */
    data class RecognizeWords(
            val uri: String,
            val requestId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.RecognitionFailed(
                errorRes = R.string.photo_words_error_recognition,
                requestId = requestId,
        )
    }

    /**
     * IS529: токены одним запросом на сервер слов (Ш6). Слой сети исключений
     * не бросает; сюда долетает только неожиданное (нет словаря) — для
     * пользователя это «сервер не ответил», приложение не падает (WFT-61).
     */
    data class LookupWords(
            val tokens: List<RecognizedWord>,
            val requestId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.WordsLookedUp(
                outcome = PhotoLookupOutcome.Unavailable,
                requestId = requestId,
        )
    }

    /**
     * IS532 (Ш1): форма словаря снимка и его леммы с текстами лемм ответа.
     * Исключение чтения — «окно не собралось» (AWP-24.1).
     */
    data class LoadPhotoForm(
            val dictionaryId: Long,
            val lemmaTexts: List<String>,
            val requestId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.PhotoFormLoaded(
                outcome = PhotoFormOutcome.Failed(cause = "${error.javaClass.simpleName}: ${error.message}"),
                requestId = requestId,
        )

        override fun toString(): String = "LoadPhotoForm(dictionaryId=$dictionaryId, lemmas=${lemmaTexts.size}, requestId=$requestId)"
    }

    /**
     * IS532 (Ш4): пакет окна одной транзакцией (AWP-20). Исключение — сбой
     * записи, транзакция откачена, окно активно с «повторить» (AWP-21).
     */
    data class AddPhotoWords(
            val batch: WordsBatch,
            val requestId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.PhotoAddRejected(
                failure = PhotoAddFailure.Failed,
                requestId = requestId,
        )

        override fun toString(): String =
            "AddPhotoWords(dictionaryId=${batch.dictionaryId}, words=${batch.words.size}, lexemes=${batch.words.sumOf { it.lexemes.size }}, requestId=$requestId)"
    }

    /** IS532 (Ш3): запомнить соответствия для словаря (AWP-06). */
    data class SavePhotoMapping(
            val dictionaryId: Long,
            val mapping: PhotoMapping,
    ) : DatasourceEffect
}

/**
 * Исполнитель эффектов вкладки «Слова»: разовые намерения (загрузка
 * paging-потока по паттерну, создание/правка/удаление слов). Живая
 * подписка на текущий словарь — не здесь: она декларируется
 * [WordsTabSub] + [WordsTabSubHandler].
 *
 * @param pagingScope scope для `cachedIn` paging-потока без фильтра
 *   (переживает пересоздание подписчиков UI); в проде это
 *   viewModelScope.
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
        private val pagingScope: CoroutineScope,
        private val wordstabUseCase: WordsTabUseCase,
        private val logger: LexemeLogger,
        private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, DatasourceEffect> {

    override val effectFamily = DatasourceEffect::class

    override suspend fun runEffect(
            effect: DatasourceEffect,
            consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        val msg = when (val eff = effect) {
            is DatasourceEffect.LoadTermFlow -> withContext(io) {
                // IS476: getCurrentDict() теперь nullable — страхуемся на случай race,
                // когда reducer уже отфильтровал null, но эффект мог быть "в пути".
                val dictionaryId = wordstabUseCase.getCurrentDict()?.id?.toInt()
                if (dictionaryId == null) {
                    Msg.NoOperation
                } else {
                    val pagingFlow = wordstabUseCase.searchTerms(
                            pattern = eff.pattern,
                            dictionaryId = dictionaryId,
                    ).let { flow ->
                        if (eff.pattern.isEmpty()) flow.cachedIn(pagingScope) else flow
                    }
                    Msg.TermsLoaded(
                            pattern = eff.pattern,
                            termList = pagingFlow,
                    )
                }
            }

            is DatasourceEffect.CreateWord -> withContext(io) {
                wordstabUseCase.addWord(eff.value)
                Msg.NoOperation
            }

            is DatasourceEffect.UpdateWord -> withContext(io) {
                async { wordstabUseCase.updateWord(eff.wordId, eff.value) }.await()
                Msg.NoOperation
            }

            is DatasourceEffect.RemoveWords -> {
                withContext(io) {
                    eff.wordSet.map { id ->
                        async { wordstabUseCase.deleteWord(id.id) }.await()
                    }
                }
                Msg.NoOperation
            }

            // IS527: полный текст по строкам — в лог: по нему на ручниках
            // сверяется качество ML Kit с фото (в списке видны только слова).
            // В магазинной сборке DEBUG никуда не уходит (LOG_LEVEL NONE,
            // CrashlyticsSink пишет только WARNING и ERROR).
            // IS529: нормализация ушла на сервер (Ш8); здесь только Ш5 —
            // пустые прочь, дубли текста, потолок 2000.
            is DatasourceEffect.RecognizeWords -> withContext(io) {
                val text = wordstabUseCase.recognizeText(Uri.parse(eff.uri))
                text.lines.forEachIndexed { index, line ->
                    logger.d(
                            tag = me.apomazkin.wordstab.LogTags.VOCAB,
                            message = "OCR line $index: $line",
                    )
                }
                Msg.TokensReady(
                        tokens = prepareTokens(text.words),
                        requestId = eff.requestId,
                )
            }

            // IS529: запрос на сервер слов; номер запуска уходит как X-Run-Id,
            // строки лога устройства и сервера склеиваются по нему.
            is DatasourceEffect.LookupWords -> withContext(io) {
                logger.d(
                        tag = me.apomazkin.wordstab.LogTags.VOCAB,
                        message = "lookup run=${eff.requestId} tokens=${eff.tokens.size}",
                )
                val outcome = wordstabUseCase.lookupWords(eff.tokens, eff.requestId)
                logger.d(
                        tag = me.apomazkin.wordstab.LogTags.VOCAB,
                        message = "lookup run=${eff.requestId} outcome=${outcome.describe()}",
                )
                Msg.WordsLookedUp(
                        outcome = outcome,
                        requestId = eff.requestId,
                )
            }

            // IS532 (Ш1): форма словаря снимка; исход компактно в лог.
            is DatasourceEffect.LoadPhotoForm -> withContext(io) {
                val outcome = wordstabUseCase.loadPhotoForm(eff.dictionaryId, eff.lemmaTexts)
                logger.d(
                        tag = me.apomazkin.wordstab.LogTags.VOCAB,
                        message = "photoForm run=${eff.requestId} dict=${eff.dictionaryId} outcome=${outcome.describe()}",
                )
                Msg.PhotoFormLoaded(
                        outcome = outcome,
                        requestId = eff.requestId,
                )
            }

            // IS532 (Ш4): запись пакета; доменный исход — в плоский Msg здесь.
            is DatasourceEffect.AddPhotoWords -> withContext(io) {
                val outcome = wordstabUseCase.addPhotoWords(eff.batch)
                logger.d(
                        tag = me.apomazkin.wordstab.LogTags.VOCAB,
                        message = "photoAdd run=${eff.requestId} outcome=${outcome.describe()}",
                )
                outcome.toMsg(eff.requestId)
            }

            is DatasourceEffect.SavePhotoMapping -> withContext(io) {
                wordstabUseCase.savePhotoMapping(eff.dictionaryId, eff.mapping)
                Msg.NoOperation
            }
        }
        consumer(msg)
    }
}

/** Короткая строка исхода для лога: вид и число слов. */

/** Доменный исход записи → плоский Msg (конвенция: reducer доменные sealed не разворачивает). */
private fun AddWordsBatchOutcome.toMsg(requestId: Long): Msg = when (this) {
    is AddWordsBatchOutcome.Added -> Msg.PhotoAddFinished(lexemeCount = lexemeIds.size, wordCount = wordIds.size, requestId = requestId)
    is AddWordsBatchOutcome.Rejected.GroupMissing -> Msg.PhotoAddRejected(PhotoAddFailure.GroupMissing(groupId), requestId)
    is AddWordsBatchOutcome.Rejected.WordMissing -> Msg.PhotoAddRejected(PhotoAddFailure.WordMissing(wordId), requestId)
    is AddWordsBatchOutcome.Rejected.TypeUnavailable -> Msg.PhotoAddRejected(PhotoAddFailure.TypeUnavailable(typeId), requestId)
    is AddWordsBatchOutcome.Rejected.NoCoreValue -> Msg.PhotoAddRejected(PhotoAddFailure.NoCoreValue, requestId)
    AddWordsBatchOutcome.Rejected.DictionaryMissing -> Msg.PhotoAddRejected(PhotoAddFailure.DictionaryMissing, requestId)
}
