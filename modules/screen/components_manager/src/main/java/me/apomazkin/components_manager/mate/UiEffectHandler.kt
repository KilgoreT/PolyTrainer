package me.apomazkin.components_manager.mate

import io.github.kilgoret.mate.MateEffectHandler
import javax.inject.Inject

/**
 * UiEffect → UiMsg маппер. Mate автоматически рассылает Msg обратно в reducer;
 * reducer (F123 retrofit) теперь обрабатывает `UiMsg.Snackbar` записывая `snackbarState`
 * в State — UI отрисует через SnackbarHost reading state.
 */
class UiEffectHandler @Inject constructor() : MateEffectHandler<Msg, UiEffect> {

    override val effectFamily = UiEffect::class

    override suspend fun runEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        val msg: Msg = when (effect) {
            is UiEffect.Snackbar -> UiMsg.Snackbar(text = effect.text)
        }
        consumer(msg)
    }
}
