package me.apomazkin.quiztab.logic

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val quizTabUseCase: QuizTabUseCase,
    private val logger: LexemeLogger,
) : MateEffectHandler<Msg, Effect> {
    
    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            null -> Msg.Empty
            else -> Msg.Empty
        }.let(consumer)
    }
}