package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import javax.inject.Inject

/**
 * UiEffect → UiMsg маппер. Mate автоматически рассылает Msg обратно в reducer;
 * reducer записывает text в `state.snackbarState` (F123). UI отрисует через SnackbarHost
 * reading state.
 */
class UiEffectHandler @Inject constructor() : MateTypedEffectHandler<Msg, UiEffect>() {

    override fun filter(effect: Effect): UiEffect? = effect as? UiEffect

    override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        val msg: Msg = when (effect) {
            is UiEffect.Snackbar -> UiMsg.Snackbar(text = effect.text)
        }
        consumer(msg)
    }
}
