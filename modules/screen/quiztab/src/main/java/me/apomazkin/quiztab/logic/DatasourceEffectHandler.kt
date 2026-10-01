package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.MateEffectHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.quiztab.LogTags
import me.apomazkin.quiztab.deps.QuizTabUseCase

/**
 * Разовые datasource-намерения таба: персист набора групп. Порядок
 * записей держит store (записи сериализованы).
 * Ошибок не ловит — провал уходит раннеру, маппинг объявлен в эффекте
 * (RecoverableEffect), стектрейс — ErrorLoggingObserver.
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
    private val useCase: QuizTabUseCase,
    private val logger: LexemeLogger,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, QuizTabDatasourceEffect> {

    override val effectFamily = QuizTabDatasourceEffect::class

    override suspend fun runEffect(
        effect: QuizTabDatasourceEffect,
        consumer: (Msg) -> Unit,
    ) {
        when (effect) {
            is QuizTabDatasourceEffect.PersistGroupSelection -> withContext(io) {
                val groups = if (effect.groupIds.isEmpty()) {
                    "all"
                } else {
                    effect.groupIds.sorted().joinToString(",")
                }
                logger.d(
                    tag = LogTags.QUIZ,
                    message = "persistGroupSelection: type=${effect.quizType} " +
                        "dict=${effect.dictionaryId} groups=$groups",
                )
                useCase.setGroupSelection(
                    quizType = effect.quizType,
                    dictionaryId = effect.dictionaryId,
                    groupIds = effect.groupIds,
                )
            }
        }
    }
}
