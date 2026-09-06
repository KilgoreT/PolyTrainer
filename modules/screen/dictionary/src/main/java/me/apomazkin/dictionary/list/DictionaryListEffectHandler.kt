package me.apomazkin.dictionary.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import javax.inject.Inject

sealed interface DictionaryListEffect : Effect {
    data class DeleteDictionary(val id: Long) : DictionaryListEffect
}

class DictionaryListEffectHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateEffectHandler<DictionaryListMsg, DictionaryListEffect> {

    override val effectFamily = DictionaryListEffect::class

    override suspend fun runEffect(
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
