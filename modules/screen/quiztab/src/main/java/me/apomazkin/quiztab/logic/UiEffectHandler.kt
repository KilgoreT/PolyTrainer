package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import javax.inject.Inject

sealed interface UiEffect : Effect {
    data class ShowSnackbar(val title: String) : UiEffect
}

class UiEffectHandler @Inject constructor() : MateEffectHandler<Msg, UiEffect> {

    override val effectFamily = UiEffect::class

    override suspend fun runEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        val msg = when (effect) {
            is UiEffect.ShowSnackbar -> UiMsg.Snackbar(
                message = effect.title,
                show = true,
            )
        }
        consumer(msg)
    }
}
