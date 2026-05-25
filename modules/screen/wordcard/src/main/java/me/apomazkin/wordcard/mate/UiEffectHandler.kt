package me.apomazkin.wordcard.mate

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import me.apomazkin.wordcard.deps.UiHost

/**
 * Обрабатывает [UiEffect]'ы через [UiHost].
 */
class UiEffectHandler @AssistedInject constructor(
    @Assisted private val uiHost: UiHost,
) : MateTypedEffectHandler<Msg, UiEffect>() {

    override fun filter(effect: Effect): UiEffect? = effect as? UiEffect

    override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        when (effect) {
            is UiEffect.ShowSnackbarWithUndo -> {
                val undoPressed = uiHost.showSnackbarWithAction(
                    messageRes = effect.messageRes,
                    actionLabelRes = effect.actionLabelRes,
                )
                if (undoPressed) consumer(effect.undoMsg)
            }
            is UiEffect.ShowErrorSnackbar -> {
                uiHost.showSnackbar(effect.messageRes)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(uiHost: UiHost): UiEffectHandler
    }
}
