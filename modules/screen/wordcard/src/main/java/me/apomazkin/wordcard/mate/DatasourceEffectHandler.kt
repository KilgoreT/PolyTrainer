package me.apomazkin.wordcard.mate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.chippicker.CategoryLabel
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.wordcard.deps.WordCardUseCase

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

    data class SaveLexeme(
        val wordId: Long,
        val category: CategoryState,
        val definition: String
    ) : DatasourceEffect

    data class ChangeLexicalCategory(
        val lexemeId: Long,
        val edited: CategoryLabel
    ) : DatasourceEffect

    data class ChangeDefinition(
        val lexemeId: Long,
        val edited: String
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
                    wordCardUseCase.getTermById(eff.wordId).let {
                        Msg.TermLoaded(it)
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
            is DatasourceEffect.DeleteLexeme -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.deleteLexeme(eff.lexemeId).let {
                        Msg.TermLoading
                    }
                }
            }

            is DatasourceEffect.SaveLexeme -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.addLexeme(
                        wordId = eff.wordId,
                        category = eff.category.edited.stringValue,
                        definition = eff.definition
                    ).let {
                        Msg.TermLoading
                    }
                }
            }

            is DatasourceEffect.ChangeDefinition -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.updateLexicalDefinition(eff.lexemeId, eff.edited).let {
                        Msg.TermLoading
                    }
                }
            }
            is DatasourceEffect.ChangeLexicalCategory -> {
                withContext(Dispatchers.IO) {
                    wordCardUseCase.updateLexicalCategory(eff.lexemeId, eff.edited.stringValue)
                        .let {
                            Msg.TermLoading
                        }
                }
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}