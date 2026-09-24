package me.apomazkin.dictionary.list

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler

sealed interface DictionaryListEffect : Effect {
    data class DeleteDictionary(val id: Long) : DictionaryListEffect
}

/**
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DictionaryListEffectHandler(
    private val dictionaryUseCase: DictionaryUseCase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<DictionaryListMsg, DictionaryListEffect> {

    override val effectFamily = DictionaryListEffect::class

    override suspend fun runEffect(
        effect: DictionaryListEffect,
        consumer: (DictionaryListMsg) -> Unit,
    ) {
        val msg = when (effect) {
            is DictionaryListEffect.DeleteDictionary -> {
                withContext(io) {
                    dictionaryUseCase.deleteDictionary(effect.id)
                }
                DictionaryListMsg.DictionaryDeleted
            }
        }
        consumer(msg)
    }
}
