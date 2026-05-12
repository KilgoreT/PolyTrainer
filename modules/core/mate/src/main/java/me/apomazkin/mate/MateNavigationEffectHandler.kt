package me.apomazkin.mate

/**
 * Базовый класс для NavigationEffectHandler.
 * Обрабатывает базовый NavigationEffect.Back, делегирует specific эффекты в onScreenEffect.
 */
abstract class MateNavigationEffectHandler<Msg>(
    protected val navigator: Navigator,
) : MateTypedEffectHandler<Msg, NavigationEffect>() {

    final override fun filter(effect: Effect): NavigationEffect? = effect as? NavigationEffect

    final override suspend fun onEffect(effect: NavigationEffect, consumer: (Msg) -> Unit) {
        when (effect) {
            is NavigationEffect.Back -> navigator.back()
            else -> onScreenEffect(effect)
        }
    }

    protected abstract suspend fun onScreenEffect(effect: NavigationEffect)
}
