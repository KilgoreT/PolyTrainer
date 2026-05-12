package me.apomazkin.dictionary.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import javax.inject.Inject

sealed interface DictionaryListEffect : Effect {
    data class DeleteDictionary(val id: Long) : DictionaryListEffect
}

class DictionaryListEffectHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateTypedEffectHandler<DictionaryListMsg, DictionaryListEffect>() {

    override fun filter(effect: Effect): DictionaryListEffect? = effect as? DictionaryListEffect

    override suspend fun onEffect(
        effect: DictionaryListEffect,
        consumer: (DictionaryListMsg) -> Unit,
    ) {
        val msg = when (effect) {
            is DictionaryListEffect.DeleteDictionary -> {
                withContext(Dispatchers.IO) {
                    dictionaryUseCase.deleteDictionary(effect.id)
                }
                DictionaryListMsg.DictionaryDeleted
            }
        }
        consumer(msg)
    }
}
