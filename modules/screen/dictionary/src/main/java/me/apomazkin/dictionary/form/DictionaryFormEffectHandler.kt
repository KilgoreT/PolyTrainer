package me.apomazkin.dictionary.form

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler

sealed interface DictionaryFormEffect : Effect {
    data class LoadDictionary(val id: Long) : DictionaryFormEffect
    /** IS525: полный список языков для окна выбора. */
    data object LoadLanguages : DictionaryFormEffect
    data class SaveDictionary(
        val name: String,
        val numericCode: Int?,
        val learningLanguage: String,
        val translationLanguage: String,
    ) : DictionaryFormEffect
    data class UpdateDictionary(
        val id: Long,
        val name: String,
        val numericCode: Int?,
        val learningLanguage: String,
        val translationLanguage: String,
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
                DictionaryFormMsg.DictionaryLoaded(
                    name = item.name,
                    flag = flag,
                    learningLanguage = item.learningLanguage,
                    translationLanguage = item.translationLanguage,
                )
            }

            is DictionaryFormEffect.LoadLanguages -> {
                val all = withContext(io) { dictionaryUseCase.allLanguages() }
                DictionaryFormMsg.LanguagesLoaded(all)
            }

            is DictionaryFormEffect.SaveDictionary -> {
                withContext(io) {
                    dictionaryUseCase.addDictionary(
                        name = effect.name,
                        numericCode = effect.numericCode,
                        learningLanguage = effect.learningLanguage,
                        translationLanguage = effect.translationLanguage,
                    )
                }
                DictionaryFormMsg.DictionarySaved
            }

            is DictionaryFormEffect.UpdateDictionary -> {
                withContext(io) {
                    dictionaryUseCase.updateDictionary(
                        id = effect.id,
                        name = effect.name,
                        numericCode = effect.numericCode,
                        learningLanguage = effect.learningLanguage,
                        translationLanguage = effect.translationLanguage,
                    )
                }
                DictionaryFormMsg.DictionarySaved
            }
        }
        consumer(msg)
    }
}
