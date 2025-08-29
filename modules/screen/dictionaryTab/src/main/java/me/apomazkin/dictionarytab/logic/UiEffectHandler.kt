package me.apomazkin.dictionarytab.logic

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

/**
 * Effect
 */
internal sealed interface UiEffect : Effect {
    data class ShowNotification(val message: String) : UiEffect
}

/**
 * EffectHandler
 */
internal class UiEffectHandler :
    MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        return when (val eff = effect as? UiEffect) {
            is UiEffect.ShowNotification -> UiMsg.ShowNotification(
                message = eff.message,
                show = true,
            )

            null -> Msg.NoOperation
        }.let(consumer)
    }
}