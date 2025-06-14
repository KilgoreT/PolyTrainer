package me.apomazkin.dictionarytab.logic

import android.util.Log
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.BuildConfig
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.EMPTY_STRING
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
    data class LoadTermFlow(
            val pattern: String = EMPTY_STRING,
    ) : DatasourceEffect

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
        private val scope: CoroutineScope,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
            effect: Effect,
            consumer: (Msg) -> Unit,
    ) {
        //TODO kilg 14.06.2025 00:48
        // https://github.com/KilgoreT/PolyTrainer/issues/372
        if (BuildConfig.DEBUG) {
            Log.d("##MATE##", "RunEffect: $effect")
        }
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

            //TODO kilg 25.05.2025 03:16 В префах хранить именно id текущего языка,
            // а не numericCode. И вообще, кешировать бы его, а не запрашивать каждый раз.
            // https://github.com/KilgoreT/PolyTrainer/issues/369
            is DatasourceEffect.LoadTermFlow -> {
                withContext(Dispatchers.IO) {
                    val langId = dictionaryTabUseCase.getLangId(
                            numericCode = dictionaryTabUseCase.getCurrentDict().numericCode
                    )
                    val pagingFlow = dictionaryTabUseCase.searchTerms(
                            pattern = eff.pattern,
                            langId = langId
                    ).let { flow ->
                        if (eff.pattern.isEmpty()) flow.cachedIn(scope) else flow
                    }
                    Msg.TermDataLoaded(
                            pattern = eff.pattern,
                            termList = pagingFlow,
                    )

                }
            }

            is DatasourceEffect.AddWord -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase
                            .addWord(eff.value)
                            .let { Msg.Empty }
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
                            .let { Msg.Empty }
                }
            }

            is DatasourceEffect.DeleteWord -> {
                withContext(Dispatchers.IO) {
                    eff.wordSet.map { id ->
                        async { dictionaryTabUseCase.deleteWord(id.id) }.await()
                    }
                }.let { Msg.Empty }
            }

            is DatasourceEffect.SaveLexeme -> {
                withContext(Dispatchers.IO) {
                    // TODO: Проследить, чтобы обновлялись только те лексемы,
                    //  которые реально были изменены
                    //  https://github.com/KilgoreT/PolyTrainer/issues/374
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
                    }.let { Msg.Empty }
                }
            }

            is DatasourceEffect.DeleteLexeme -> {
                withContext(Dispatchers.IO) {
                    dictionaryTabUseCase.deleteLexeme(eff.lexemeId)
                            .let { Msg.Empty }
                }
            }

            null -> Msg.Empty
        }.let(consumer)
    }
}