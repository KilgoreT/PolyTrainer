package me.apomazkin.dictionary.form

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.mate.NavigationEffect

class NavigationEffectHandler(
    private val onBack: () -> Unit,
) : MateEffectHandler<DictionaryFormMsg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (DictionaryFormMsg) -> Unit,
    ) {
        val msg = when (effect) {
            is NavigationEffect.Back -> {
                onBack()
                DictionaryFormMsg.Empty
            }
            else -> DictionaryFormMsg.Empty
        }
        consumer(msg)
    }
}
