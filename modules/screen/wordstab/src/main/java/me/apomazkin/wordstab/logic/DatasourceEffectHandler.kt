package me.apomazkin.wordstab.logic

import android.net.Uri
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.wordstab.logic.photo.extractWords
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
            is DatasourceEffect.RecognizeWords -> withContext(io) {
                val text = wordstabUseCase.recognizeText(Uri.parse(eff.uri))
                text.lines.forEachIndexed { index, line ->
                    logger.d(
                            tag = me.apomazkin.wordstab.LogTags.VOCAB,
                            message = "OCR line $index: $line",
                    )
                }
                Msg.WordsRecognized(
                        words = extractWords(text.words),
                        requestId = eff.requestId,
                )
            }
        }
        consumer(msg)
    }
}
