package me.apomazkin.vocabulary.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.vocabulary.deps.VocabularyUseCase
import me.apomazkin.vocabulary.entity.WordInfo

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {

    /**
     * Effect to get available languages.
     */
    data object LoadDictList : DatasourceEffect

    /**
     * Effect to get current language.
     */
    data object LoadCurrentDict : DatasourceEffect

    /**
     * Effect to change current language.
     */
    data class ChangeDict(val numericCode: Int) : DatasourceEffect

    /**
     * Effect to load data.
     */
    data object LoadTermData : DatasourceEffect

    /**
     * Effect to add new word.
     */
    data class AddWord(val value: String) : DatasourceEffect
    data class ChangeWord(val wordId: Long, val value: String) : DatasourceEffect

    /**
     * Effect to delete word.
     */
    data class DeleteWord(val wordSet: Set<WordInfo>) : DatasourceEffect

    /**
     * Effect to save(add or update) lexeme.
     */
    data class SaveLexeme(
        val wordId: Long,
        val lexemeList: List<LexemeState>,
    ) : DatasourceEffect

    data class DeleteLexeme(val lexemeId: Long) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val vocabularyUseCase: VocabularyUseCase,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadDictList -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.getAvailableDict().let {
                        TopBarActionMsg.AvailableDict(list = it)
                    }
                }
            }

            is DatasourceEffect.LoadCurrentDict -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.getCurrentDict()
                        .let { TopBarActionMsg.CurrentDict(numericCode = it) }
                }
            }

            is DatasourceEffect.ChangeDict -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.changeDict(numericCode = eff.numericCode)
                        .let { TopBarActionMsg.CurrentDict(numericCode = eff.numericCode) }
                }
            }

            is DatasourceEffect.LoadTermData -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.getWordList()
                        .let { Msg.TermDataLoaded(termList = it) }
                }
            }

            is DatasourceEffect.AddWord -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.addWord(eff.value).let { Msg.TermDataLoad }
                }
            }

            is DatasourceEffect.ChangeWord -> {
                withContext(Dispatchers.IO) {
                    async { vocabularyUseCase.updateWord(eff.wordId, eff.value) }
                        .await()
                        .let { if (it) Msg.TermDataLoad else Msg.TermDataLoad }
                }
            }

            is DatasourceEffect.DeleteWord -> {
                withContext(Dispatchers.IO) {
                    eff.wordSet.map { id ->
                        async { vocabularyUseCase.deleteWord(id.id) }.await()
                    }
                }.let {
                    Msg.TermDataLoad
                }
            }

            is DatasourceEffect.SaveLexeme -> {
                withContext(Dispatchers.IO) {
                    // TODO: Проследить, чтобы обновлялись только те лексемы, которые реально были изменены
                    eff.lexemeList.forEach { lexeme ->
                        lexeme.lexemeId?.let { lexemeId ->
                            vocabularyUseCase.editLexeme(
                                eff.wordId,
                                lexemeId,
                                lexeme.category.stringValue,
                                lexeme.definition.editedText,
                            )
                        } ?: run {
                            vocabularyUseCase.addLexeme(
                                eff.wordId,
                                lexeme.category.stringValue,
                                lexeme.definition.editedText,
                            )
                        }
                    }.let { Msg.TermDataLoad }
                }
            }

            is DatasourceEffect.DeleteLexeme -> {
                withContext(Dispatchers.IO) {
                    vocabularyUseCase.deleteLexeme(eff.lexemeId)
                        .let { Msg.TermDataLoad }
                }
            }

            null -> Msg.Empty
        }.let(consumer)
    }
}