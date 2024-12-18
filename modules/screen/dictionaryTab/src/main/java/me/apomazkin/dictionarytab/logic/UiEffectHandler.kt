package me.apomazkin.dictionarytab.logic

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

/**
 * Effect
 */
internal sealed interface UiEffect : Effect {
    data class ShowSnackbar(val title: String) : UiEffect
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
            is UiEffect.ShowSnackbar -> UiMsg.Snackbar(message = eff.title, show = true)
            null -> Msg.Empty
        }.let(consumer)
    }
}