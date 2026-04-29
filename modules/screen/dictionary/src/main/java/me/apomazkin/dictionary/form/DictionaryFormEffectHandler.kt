package me.apomazkin.dictionary.form

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

sealed interface DictionaryFormEffect : Effect {
    data class FilterFlags(val query: String) : DictionaryFormEffect
    data class LoadDictionary(val id: Long) : DictionaryFormEffect
    data class SaveDictionary(val name: String, val numericCode: Int?) : DictionaryFormEffect
    data class UpdateDictionary(
        val id: Long,
        val name: String,
        val numericCode: Int?,
    ) : DictionaryFormEffect
}

class DictionaryFormEffectHandler(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateEffectHandler<DictionaryFormMsg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (DictionaryFormMsg) -> Unit
    ) {
        val msg = when (val e = effect as? DictionaryFormEffect) {
            is DictionaryFormEffect.LoadDictionary -> {
                val item = withContext(Dispatchers.IO) {
                    dictionaryUseCase.getDictionary(e.id)
                }
                val flag = item.numericCode?.let { dictionaryUseCase.findFlag(it) }
                DictionaryFormMsg.DictionaryLoaded(item.name, flag)
            }

            is DictionaryFormEffect.SaveDictionary -> {
                withContext(Dispatchers.IO) {
                    dictionaryUseCase.addDictionary(e.name, e.numericCode)
                }
                DictionaryFormMsg.DictionarySaved
            }

            is DictionaryFormEffect.UpdateDictionary -> {
                withContext(Dispatchers.IO) {
                    dictionaryUseCase.updateDictionary(e.id, e.name, e.numericCode)
                }
                DictionaryFormMsg.DictionarySaved
            }

            is DictionaryFormEffect.FilterFlags -> DictionaryFormMsg.Empty

            null -> DictionaryFormMsg.Empty
        }
        consumer(msg)
    }
}
