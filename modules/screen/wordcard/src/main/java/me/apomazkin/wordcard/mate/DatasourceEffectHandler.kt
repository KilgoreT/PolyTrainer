package me.apomazkin.wordcard.mate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.entity.Lexeme

internal sealed interface DatasourceEffect : Effect {

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

internal class DatasourceEffectHandler(
    private val wordCardUseCase: WordCardUseCase,
) : MateEffectHandler<Msg, Effect> {
    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadWord -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.getTermById(eff.wordId).let { term ->
                        term?.let {
                            Msg.WordLoaded(it)
                        } ?: Msg.WordNotFound
                    }
                }
            }
            is DatasourceEffect.RemoveWord -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteWord(eff.wordId).let {
                        Msg.NavigateBack
                    }
                }
            }
            is DatasourceEffect.UpdateWord -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.updateWord(eff.wordId, eff.value).let {
                        Msg.LoadingWord
                    }
                }
            }
            is DatasourceEffect.CreateLexeme -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.addLexeme(eff.wordId).let { lexeme: Lexeme? ->
                        lexeme
                            ?.let { Msg.RefreshLexeme(it) }
                            ?: throw IllegalStateException("Lexeme not found")
                    }
                }
            }

            is DatasourceEffect.UpdateLexemeTranslation -> {
                withContext(Dispatchers.IO) {
                    val lexemeId = if (eff.lexemeId > -1) eff.lexemeId else null
                    wordCardUseCase.addLexemeTranslation(
                        wordId = eff.wordId,
                        lexemeId = lexemeId,
                        translation = eff.translation
                    ).let { lexeme: Lexeme? ->
                        lexeme
                            ?.let { Msg.RefreshTranslation(it) }
                            ?: throw IllegalStateException("Lexeme not found")
                    }
                }
            }

            is DatasourceEffect.UpdateLexemeDefinition -> {
                withContext(Dispatchers.IO) {
                    val lexemeId = if (eff.lexemeId > -1) eff.lexemeId else null
                    wordCardUseCase.addLexemeDefinition(
                        wordId = eff.wordId,
                        lexemeId = lexemeId,
                        definition = eff.definition
                    ).let { lexeme: Lexeme? ->
                        lexeme
                            ?.let { Msg.RefreshDefinition(it) }
                            ?: throw IllegalStateException("Lexeme not found")
                    }
                }
            }

            is DatasourceEffect.RemoveTranslation -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteLexemeTranslation(eff.lexemeId).let {
                        Msg.LoadingWord
                    }
                }
            }

            is DatasourceEffect.RemoveDefinition -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteLexemeDefinition(eff.lexemeId).let {
                        Msg.LoadingWord
                    }
                }
            }

            is DatasourceEffect.RemoveLexeme -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteLexeme(eff.lexemeId).let {
                        Msg.LoadingWord
                    }
                }
            }
            null -> Msg.NoOperation
        }.let(consumer)
    }
}