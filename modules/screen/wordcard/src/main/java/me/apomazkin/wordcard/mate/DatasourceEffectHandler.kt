package me.apomazkin.wordcard.mate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Lexeme
import javax.inject.Inject

sealed interface DatasourceEffect : Effect {

    data class LoadWord(
        val wordId: Long
    ) : DatasourceEffect

    data class RemoveWord(
        val wordId: Long
    ) : DatasourceEffect

    data class UpdateWord(
        val wordId: Long,
        val value: String
    ) : DatasourceEffect

    data class CreateLexeme(
        val wordId: Long,
    ) : DatasourceEffect

    data class UpdateLexemeTranslation(
        val wordId: Long,
        val lexemeId: Long,
        val translation: String
    ) : DatasourceEffect

    data class UpdateLexemeDefinition(
        val wordId: Long,
        val lexemeId: Long,
        val definition: String
    ) : DatasourceEffect

    data class RemoveTranslation(
        val lexemeId: Long
    ) : DatasourceEffect

    data class RemoveDefinition(
        val lexemeId: Long
    ) : DatasourceEffect

    data class RemoveLexeme(
        val lexemeId: Long
    ) : DatasourceEffect
}

class DatasourceEffectHandler @Inject constructor(
    private val wordCardUseCase: WordCardUseCase,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        val msg: Msg = when (effect) {
            is DatasourceEffect.LoadWord -> withContext(Dispatchers.IO) {
                wordCardUseCase.getTermById(effect.wordId)?.let { Msg.WordLoaded(it) }
                    ?: Msg.WordNotFound
            }
            is DatasourceEffect.RemoveWord -> withContext(Dispatchers.IO) {
                wordCardUseCase.deleteWord(effect.wordId)
                Msg.NavigateBack
            }
            is DatasourceEffect.UpdateWord -> withContext(Dispatchers.IO) {
                wordCardUseCase.updateWord(effect.wordId, effect.value)
                Msg.LoadingWord
            }
            is DatasourceEffect.CreateLexeme -> withContext(Dispatchers.IO) {
                wordCardUseCase.addLexeme(effect.wordId)
                    ?.let { Msg.RefreshLexeme(it) }
                    ?: throw IllegalStateException("Lexeme not found")
            }
            is DatasourceEffect.UpdateLexemeTranslation -> withContext(Dispatchers.IO) {
                val lexemeId = if (effect.lexemeId > -1) effect.lexemeId else null
                wordCardUseCase.addLexemeTranslation(
                    wordId = effect.wordId,
                    lexemeId = lexemeId,
                    translation = effect.translation,
                )?.let { Msg.RefreshTranslation(it) }
                    ?: throw IllegalStateException("Lexeme not found")
            }
            is DatasourceEffect.UpdateLexemeDefinition -> withContext(Dispatchers.IO) {
                val lexemeId = if (effect.lexemeId > -1) effect.lexemeId else null
                wordCardUseCase.addLexemeDefinition(
                    wordId = effect.wordId,
                    lexemeId = lexemeId,
                    definition = effect.definition,
                )?.let { Msg.RefreshDefinition(it) }
                    ?: throw IllegalStateException("Lexeme not found")
            }
            is DatasourceEffect.RemoveTranslation -> withContext(Dispatchers.IO) {
                wordCardUseCase.deleteLexemeTranslation(effect.lexemeId)
                Msg.LoadingWord
            }
            is DatasourceEffect.RemoveDefinition -> withContext(Dispatchers.IO) {
                wordCardUseCase.deleteLexemeDefinition(effect.lexemeId)
                Msg.LoadingWord
            }
            is DatasourceEffect.RemoveLexeme -> withContext(Dispatchers.IO) {
                wordCardUseCase.deleteLexeme(effect.lexemeId)
                Msg.LoadingWord
            }
        }
        consumer(msg)
    }
}
