package me.apomazkin.dictionary.form

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler

sealed interface DictionaryFormEffect : Effect {
    data class LoadDictionary(val id: Long) : DictionaryFormEffect
    data class SaveDictionary(val name: String, val numericCode: Int?) : DictionaryFormEffect
    data class UpdateDictionary(
        val id: Long,
        val name: String,
        val numericCode: Int?,
    ) : DictionaryFormEffect
}

/**
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DictionaryFormEffectHandler(
    private val dictionaryUseCase: DictionaryUseCase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<DictionaryFormMsg, DictionaryFormEffect> {

    override val effectFamily = DictionaryFormEffect::class

    override suspend fun runEffect(effect: DictionaryFormEffect, consumer: (DictionaryFormMsg) -> Unit) {
        val msg = when (effect) {
            is DictionaryFormEffect.LoadDictionary -> {
                val item = withContext(io) {
                    dictionaryUseCase.getDictionary(effect.id)
                }
                val flag = item.numericCode?.let { dictionaryUseCase.findFlag(it) }
                DictionaryFormMsg.DictionaryLoaded(item.name, flag)
            }

            is DictionaryFormEffect.SaveDictionary -> {
                withContext(io) {
                    dictionaryUseCase.addDictionary(effect.name, effect.numericCode)
                }
                DictionaryFormMsg.DictionarySaved
            }

            is DictionaryFormEffect.UpdateDictionary -> {
                withContext(io) {
                    dictionaryUseCase.updateDictionary(effect.id, effect.name, effect.numericCode)
                }
                DictionaryFormMsg.DictionarySaved
            }
        }
        consumer(msg)
    }
}
