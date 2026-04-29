package me.apomazkin.dictionary.list

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.mate.NavigationEffect

class ListNavigationEffectHandler(
    private val onBackPress: (() -> Unit)?,
    private val onExit: (() -> Unit)?,
    private val onEditDictionary: (Long) -> Unit,
) : MateEffectHandler<DictionaryListMsg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (DictionaryListMsg) -> Unit,
    ) {
        val msg = when (effect) {
            is ListNavigationEffect.OpenEdit -> {
                onEditDictionary(effect.id)
                DictionaryListMsg.Empty
            }
            is NavigationEffect.Back -> {
                onBackPress?.invoke()
                DictionaryListMsg.Empty
            }
            is NavigationEffect.ExitApp -> {
                onExit?.invoke()
                DictionaryListMsg.Empty
            }
            else -> DictionaryListMsg.Empty
        }
        consumer(msg)
    }
}
