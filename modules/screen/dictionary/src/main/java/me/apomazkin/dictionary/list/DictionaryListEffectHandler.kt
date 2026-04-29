package me.apomazkin.dictionary.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

sealed interface DictionaryListEffect : Effect {
    data class DeleteDictionary(val id: Long) : DictionaryListEffect
}

class DictionaryListEffectHandler(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateEffectHandler<DictionaryListMsg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (DictionaryListMsg) -> Unit
    ) {
        val msg = when (val e = effect as? DictionaryListEffect) {
            is DictionaryListEffect.DeleteDictionary -> {
                withContext(Dispatchers.IO) {
                    dictionaryUseCase.deleteDictionary(e.id)
                }
                DictionaryListMsg.DictionaryDeleted
            }

            null -> DictionaryListMsg.Empty
        }
        consumer(msg)
    }
}
