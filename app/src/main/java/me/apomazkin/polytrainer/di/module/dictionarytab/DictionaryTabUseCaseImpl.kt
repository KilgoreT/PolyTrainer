package me.apomazkin.polytrainer.di.module.dictionarytab

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.deps.DictionaryNotFoundException
import me.apomazkin.dictionarytab.entity.DefinitionUiEntity
import me.apomazkin.dictionarytab.entity.LexemeUiItem
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.TranslationUiEntity
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val wordApi: CoreDbApi.WordApi,
    private val termApi: CoreDbApi.TermApi,
    private val prefsProvider: PrefsProvider,
    private val flagProvider: FlagProvider,
) : DictionaryTabUseCase {

    override suspend fun getDictionaryId(numericCode: Int): Int {
        return dictionaryApi.getDictionary(numericCode)?.id?.toInt() ?: 0
    }

    override suspend fun getCurrentDict(): DictUiEntity {
        prefsProvider
            .getInt(PrefKey.CURRENT_DICTIONARY_ID_LONG)?.let { num ->
                dictionaryApi.getDictionary(numericCode = num)?.let {
                    return DictUiEntity(
                        flagRes = flagProvider.getFlagRes(it.numericCode ?: 0),
                        title = it.name,
                        numericCode = it.numericCode ?: 0,
                    )
                }
            } ?: dictionaryApi.getDictionaryList()
            .firstOrNull()
            ?.let {
                prefsProvider.setInt(
                    PrefKey.CURRENT_DICTIONARY_ID_LONG,
                    it.numericCode ?: 0
                )
                return DictUiEntity(
                    flagRes = flagProvider.getFlagRes(it.numericCode ?: 0),
                    title = it.name,
                    numericCode = it.numericCode ?: 0,
                )
            }
        throw DictionaryNotFoundException()
    }

    override fun flowCurrentDict(): Flow<DictUiEntity> = prefsProvider
        .getIntFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        .map { numeric: Int ->
            val dict = (dictionaryApi
                .getDictionary(numeric)
                ?: dictionaryApi.getDictionaryList().firstOrNull())
                ?.let { dict ->
                    DictUiEntity(
                        flagRes = flagProvider.getFlagRes(dict.numericCode ?: 0),
                        title = dict.name,
                        numericCode = dict.numericCode ?: 0,
                    )
                }
            dict ?: throw DictionaryNotFoundException()
        }

    // TODO: Убрать нулеабельность в Dictionary: id и name
    override suspend fun addWord(value: String): Long =
        prefsProvider
            .getInt(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { numericCode ->
                dictionaryApi.getDictionary(numericCode)?.let {
                    return wordApi.addWordSuspend(value, it.id.toInt())
                } ?: throw IllegalStateException("Dictionary not found")
            } ?: throw IllegalStateException("Dictionary not found")

    override suspend fun updateWord(id: Long, value: String): Boolean =
        wordApi.updateWordSuspend(id, value)

    override suspend fun deleteWord(wordId: Long) {
        wordApi.deleteWordSuspend(wordId)
    }

    override suspend fun changeDict(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_DICTIONARY_ID_LONG, numericCode)
    }

    // TODO: Передавать dictionaryId сюда в параметр. нехер делать лишний запрос
    // dictionaryId хранить в стейте
    override suspend fun getWordList(): List<TermUiItem> {
        return prefsProvider
            .getInt(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { numericCode ->
                dictionaryApi.getDictionary(numericCode = numericCode)?.id?.toInt()?.let { dictionaryId: Int ->
                    termApi.getTermList(dictionaryId)
                        .map { term ->
                            TermUiItem(
                                id = term.word.id,
                                wordValue = term.word.value,
                                dictionaryId = term.word.dictionaryId,
                                addDate = term.word.addDate,
                                changeDate = term.word.changeDate,
                                lexemeList = term.lexemes.map { defMate ->
                                    LexemeUiItem(
                                        id = defMate.id,
                                        wordId = defMate.wordId,
                                        translation = defMate.translation?.let {
                                            TranslationUiEntity(
                                                it.value
                                            )
                                        },
                                        definition = defMate.definition?.let { DefinitionUiEntity(it.value) },
                                        addDate = defMate.addDate,
                                        changeDate = defMate.changeDate,
                                    )
                                }
                            )
                        }
                } ?: throw IllegalStateException("Dictionary not found")
            } ?: throw IllegalStateException("Dictionary not found")
    }

    override fun searchTerms(pattern: String, dictionaryId: Int): Flow<PagingData<TermUiItem>> {
        return termApi.searchTermsPaging(
            pattern = pattern,
            dictionaryId = dictionaryId
        ).map { pagingData ->
            pagingData.map { term ->
                TermUiItem(
                    id = term.word.id,
                    wordValue = term.word.value,
                    dictionaryId = term.word.dictionaryId,
                    addDate = term.word.addDate,
                    changeDate = term.word.changeDate,
                    lexemeList = term.lexemes.map { defMate ->
                        LexemeUiItem(
                            id = defMate.id,
                            wordId = defMate.wordId,
                            translation = defMate.translation?.let {
                                TranslationUiEntity(
                                    it.value
                                )
                            },
                            definition = defMate.definition?.let { DefinitionUiEntity(it.value) },
                            addDate = defMate.addDate,
                            changeDate = defMate.changeDate,
                        )
                    }
                )
            }

        }
    }
}