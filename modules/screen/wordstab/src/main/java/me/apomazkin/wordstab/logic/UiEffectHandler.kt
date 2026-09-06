package me.apomazkin.wordstab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import javax.inject.Inject

sealed interface UiEffect : Effect {
    data class ShowNotification(val message: String) : UiEffect
}

class UiEffectHandler @Inject constructor() : MateEffectHandler<Msg, UiEffect> {

    override val effectFamily = UiEffect::class

    override suspend fun runEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        val msg = when (effect) {
            is UiEffect.ShowNotification -> UiMsg.ShowNotification(
                message = effect.message,
                show = true,
            )
        }
        consumer(msg)
    }
}
