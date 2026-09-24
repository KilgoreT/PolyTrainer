package me.apomazkin.dictionaryappbar.mate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger

sealed interface DatasourceEffect : Effect {
    data class ChangeDict(val dict: DictUiEntity) : DatasourceEffect
}

/**
 * Исполнитель эффектов-мутаций app bar'а: разовое намерение «сменить
 * текущий словарь». Живые списки словарей — не здесь: они длящиеся и
 * декларируются подписками [DictionaryAppBarSub] +
 * [DictionaryAppBarSubHandler].
 *
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
        private val useCase: DictionaryAppBarUseCase,
        private val logger: LexemeLogger,
        private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, DatasourceEffect> {

    override val effectFamily = DatasourceEffect::class

    override suspend fun runEffect(
            effect: DatasourceEffect,
            consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        val msg = when (effect) {
            is DatasourceEffect.ChangeDict -> {
                withContext(io) {
                    useCase.changeDict(id = effect.dict.id)
                }
                Msg.Empty
            }
        }
        consumer(msg)
    }
}
