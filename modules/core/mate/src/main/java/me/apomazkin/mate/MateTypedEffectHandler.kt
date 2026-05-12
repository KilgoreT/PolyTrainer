package me.apomazkin.mate

/**
 * Базовый класс для EffectHandler с типизированной фильтрацией эффектов.
 * Mate вызывает runEffect() на КАЖДОМ handler с КАЖДЫМ эффектом — этот класс
 * автоматически отфильтровывает чужие через filter() и вызывает onEffect() только для своих.
 *
 * Если effect не относится к типу E (filter вернул null) — handler выходит,
 * consumer не вызывается. Mate продолжает цикл со следующим handler.
 */
abstract class MateTypedEffectHandler<Msg, E : Effect> : MateEffectHandler<Msg, Effect> {

    final override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        val typed = filter(effect) ?: return
        onEffect(typed, consumer)
    }

    protected abstract fun filter(effect: Effect): E?
    protected abstract suspend fun onEffect(effect: E, consumer: (Msg) -> Unit)
}
