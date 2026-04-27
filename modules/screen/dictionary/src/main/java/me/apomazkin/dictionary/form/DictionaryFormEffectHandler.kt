package me.apomazkin.dictionary.form

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

sealed interface DictionaryFormEffect : Effect {
    data object LoadLanguages : DictionaryFormEffect
    data class LoadFlagsForLanguage(val languageCode: String) : DictionaryFormEffect
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
            is DictionaryFormEffect.LoadLanguages -> {
                val list = dictionaryUseCase.getAvailableLanguages()
                DictionaryFormMsg.LanguagesLoaded(list)
            }

            is DictionaryFormEffect.LoadFlagsForLanguage -> {
                val list = withContext(Dispatchers.IO) {
                    dictionaryUseCase.getCountriesForLanguage(e.languageCode)
                }
                DictionaryFormMsg.FlagsLoaded(list)
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

            null -> DictionaryFormMsg.Empty
        }
        consumer(msg)
    }
}
