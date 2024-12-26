package me.apomazkin.dictionarytab.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

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
    data class ChangeDict(val lang: DictUiEntity) : DatasourceEffect

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
    private val dictionaryTabUseCase: DictionaryTabUseCase,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadDictList -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase.getAvailableDict().let {
                        TopBarActionMsg.AvailableDict(list = it)
                    }
                }
            }

            is DatasourceEffect.LoadCurrentDict -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase.getCurrentDict()
                        .let { TopBarActionMsg.CurrentDict(lang = it) }
                }
            }

            is DatasourceEffect.ChangeDict -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase
                        .changeDict(numericCode = eff.lang.numericCode)
                        .let { TopBarActionMsg.CurrentDict(lang = eff.lang) }
                }
            }

            is DatasourceEffect.LoadTermData -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase.getWordList()
                        .let { Msg.TermDataLoaded(termList = it) }
                }
            }

            is DatasourceEffect.AddWord -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase
                        .addWord(eff.value)
                        .let { Msg.TermDataLoad }
                }
            }

            is DatasourceEffect.ChangeWord -> {
                withContext(Dispatchers.IO) {
                    async {
                        dictionaryTabUseCase.updateWord(
                            eff.wordId,
                            eff.value
                        )
                    }
                        .await()
                        .let { if (it) Msg.TermDataLoad else Msg.TermDataLoad }
                }
            }

            is DatasourceEffect.DeleteWord -> {
                withContext(Dispatchers.IO) {
                    eff.wordSet.map { id ->
                        async { dictionaryTabUseCase.deleteWord(id.id) }.await()
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
                            dictionaryTabUseCase.editLexeme(
                                eff.wordId,
                                lexemeId,
                                lexeme.category.stringValue,
                                lexeme.definition.editedText,
                            )
                        } ?: run {
                            dictionaryTabUseCase.addLexeme(
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
                    dictionaryTabUseCase.deleteLexeme(eff.lexemeId)
                        .let { Msg.TermDataLoad }
                }
            }

            null -> Msg.Empty
        }.let(consumer)
    }
}