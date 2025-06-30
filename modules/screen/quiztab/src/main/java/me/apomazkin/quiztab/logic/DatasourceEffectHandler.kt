package me.apomazkin.quiztab.logic

import android.util.Log
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.quiztab.deps.QuizTabUseCase

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
) : MateEffectHandler<Msg, Effect> {
    
    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            null -> Msg.Empty
            else -> Msg.Empty
        }.let(consumer)
    }
}