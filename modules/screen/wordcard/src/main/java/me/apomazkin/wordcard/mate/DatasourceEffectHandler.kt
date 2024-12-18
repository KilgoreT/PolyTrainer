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

    data class DeleteWord(
        val wordId: Long
    ) : DatasourceEffect

    data class SaveWord(
        val wordId: Long,
        val value: String
    ) : DatasourceEffect

    data class AddLexeme(
        val wordId: Long,
    ) : DatasourceEffect

    data class SaveLexemeTranslation(
        val wordId: Long,
        val lexemeId: Long,
        val translation: String
    ) : DatasourceEffect

    data class SaveLexemeDefinition(
        val wordId: Long,
        val lexemeId: Long,
        val definition: String
    ) : DatasourceEffect

    data class DeleteTranslation(
        val lexemeId: Long
    ) : DatasourceEffect

    data class DeleteDefinition(
        val lexemeId: Long
    ) : DatasourceEffect

    data class DeleteLexeme(
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
                            Msg.TermLoaded(it)
                        } ?: Msg.TermNotLoaded
                    }
                }
            }
            is DatasourceEffect.DeleteWord -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteWord(eff.wordId).let {
                        Msg.CloseScreen
                    }
                }
            }
            is DatasourceEffect.SaveWord -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.updateWord(eff.wordId, eff.value).let {
                        Msg.TermLoading
                    }
                }
            }
            is DatasourceEffect.AddLexeme -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.addLexeme(eff.wordId).let { lexeme: Lexeme? ->
                        lexeme
                            ?.let { Msg.LexemeUpdate(it) }
                            ?: throw IllegalStateException("Lexeme not found")
                    }
                }
            }

            is DatasourceEffect.SaveLexemeTranslation -> {
                withContext(Dispatchers.IO) {
                    val lexemeId = if (eff.lexemeId > -1) eff.lexemeId else null
                    wordCardUseCase.addLexemeTranslation(
                        wordId = eff.wordId,
                        lexemeId = lexemeId,
                        translation = eff.translation
                    ).let { lexeme: Lexeme? ->
                        lexeme
                            ?.let { Msg.TranslationUpdate(it) }
                            ?: throw IllegalStateException("Lexeme not found")
                    }
                }
            }

            is DatasourceEffect.SaveLexemeDefinition -> {
                withContext(Dispatchers.IO) {
                    val lexemeId = if (eff.lexemeId > -1) eff.lexemeId else null
                    wordCardUseCase.addLexemeDefinition(
                        wordId = eff.wordId,
                        lexemeId = lexemeId,
                        definition = eff.definition
                    ).let { lexeme: Lexeme? ->
                        lexeme
                            ?.let { Msg.DefinitionUpdate(it) }
                            ?: throw IllegalStateException("Lexeme not found")
                    }
                }
            }

            is DatasourceEffect.DeleteTranslation -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteLexemeTranslation(eff.lexemeId).let {
                        Msg.TermLoading
                    }
                }
            }

            is DatasourceEffect.DeleteDefinition -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteLexemeDefinition(eff.lexemeId).let {
                        Msg.TermLoading
                    }
                }
            }

            is DatasourceEffect.DeleteLexeme -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteLexeme(eff.lexemeId).let {
                        Msg.TermLoading
                    }
                }
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}