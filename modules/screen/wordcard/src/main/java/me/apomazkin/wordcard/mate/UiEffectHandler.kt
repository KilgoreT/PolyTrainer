package me.apomazkin.wordcard.mate

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateEffectHandler
import me.apomazkin.wordcard.deps.UiHost

/**
 * Обрабатывает [UiEffect]'ы через [UiHost].
 */
class UiEffectHandler @AssistedInject constructor(
    @Assisted private val uiHost: UiHost,
) : MateEffectHandler<Msg, UiEffect> {

    override val effectFamily = UiEffect::class

    override suspend fun runEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
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
            is UiEffect.ShowSnackbarWithRetry -> {
                val retryPressed = uiHost.showSnackbarWithAction(
                    messageRes = effect.messageRes,
                    actionLabelRes = effect.actionLabelRes,
                )
                if (retryPressed) consumer(effect.retryMsg)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(uiHost: UiHost): UiEffectHandler
    }
}
