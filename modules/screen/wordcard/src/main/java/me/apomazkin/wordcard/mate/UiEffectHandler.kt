package me.apomazkin.wordcard.mate

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

/**
 * Effect
 */
internal sealed interface UiEffect : Effect {
    data class ShowNotification(val title: String) : UiEffect
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
            is UiEffect.ShowNotification -> UiMsg.ShowNotification(text = eff.title, show = true)
            null -> Msg.NoOperation
        }.let(consumer)
    }
}