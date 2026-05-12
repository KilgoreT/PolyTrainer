package me.apomazkin.settingstab.logic

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import javax.inject.Inject

sealed interface UiEffect : Effect {
    data class ShowSnackbar(val title: String) : UiEffect
}

class UiEffectHandler @Inject constructor() : MateTypedEffectHandler<Msg, UiEffect>() {

    override fun filter(effect: Effect): UiEffect? = effect as? UiEffect

    override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        val msg = when (effect) {
            is UiEffect.ShowSnackbar -> UiMsg.Snackbar(
                message = effect.title,
                show = true,
            )
        }
        consumer(msg)
    }
}