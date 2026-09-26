package me.apomazkin.per_dictionary_components.mate

import io.github.kilgoret.mate.MateEffectHandler
import javax.inject.Inject

/**
 * UiEffect → UiMsg маппер. Mate автоматически рассылает Msg обратно в reducer;
 * reducer записывает text в `state.snackbarState` (F123). UI отрисует через SnackbarHost
 * reading state.
 */
class UiEffectHandler
    @Inject
    constructor() : MateEffectHandler<Msg, UiEffect> {
        override val effectFamily = UiEffect::class

        override suspend fun runEffect(
            effect: UiEffect,
            consumer: (Msg) -> Unit,
        ) {
            val msg: Msg = when (effect) {
                is UiEffect.Snackbar -> UiMsg.Snackbar(text = effect.text)
            }
            consumer(msg)
        }
    }
