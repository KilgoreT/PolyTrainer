package me.apomazkin.polytrainer.di.module.dictionarytab

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.toUiItem
import me.apomazkin.flags.CountryProvider
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val wordApi: CoreDbApi.WordApi,
    private val termApi: CoreDbApi.TermApi,
    private val prefsProvider: PrefsProvider,
    private val countryProvider: CountryProvider,
) : DictionaryTabUseCase {

    override suspend fun getCurrentDict(): DictUiEntity? {
        prefsProvider
            .getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)?.let { id ->
                dictionaryApi.getDictionaryById(id)?.let {
                    return DictUiEntity(
                        id = it.id,
                        flagRes = it.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                        title = it.name,
                        numericCode = it.numericCode ?: 0,
                    )
                }
            } ?: dictionaryApi.getDictionaryList()
            .firstOrNull()
            ?.let {
                prefsProvider.setLong(
                    PrefKey.CURRENT_DICTIONARY_ID_LONG,
                    it.id
                )
                return DictUiEntity(
                    id = it.id,
                    flagRes = it.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                    title = it.name,
                    numericCode = it.numericCode ?: 0,
                )
            }
        // IS476: нет ни prefs, ни словарей — null вместо throw
        return null
    }

    override fun flowCurrentDict(): Flow<DictUiEntity?> = prefsProvider
        .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        .map { id: Long? ->
            // IS476: null если ни по id, ни fallback ничего не нашли — валидное состояние
            (id?.let { dictionaryApi.getDictionaryById(it) }
                ?: dictionaryApi.getDictionaryList().firstOrNull())
                ?.let { dict ->
                    DictUiEntity(
                        id = dict.id,
                        flagRes = dict.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                        title = dict.name,
                        numericCode = dict.numericCode ?: 0,
                    )
                }
        }

    // TODO: Убрать нулеабельность в Dictionary: id и name
    override suspend fun addWord(value: String): Long =
        prefsProvider
            .getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { id ->
                dictionaryApi.getDictionaryById(id)?.let {
                    return wordApi.addWordSuspend(value, it.id.toInt())
                } ?: throw IllegalStateException("Dictionary not found")
            } ?: throw IllegalStateException("Dictionary not found")

    override suspend fun updateWord(id: Long, value: String): Boolean =
        wordApi.updateWordSuspend(id, value)

    override suspend fun deleteWord(wordId: Long) {
        wordApi.deleteWordSuspend(wordId)
    }

    override suspend fun changeDict(id: Long) {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, id)
    }

    // TODO: Передавать dictionaryId сюда в параметр. нехер делать лишний запрос
    // dictionaryId хранить в стейте
    override suspend fun getWordList(): List<TermUiItem> {
        return prefsProvider
            .getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            ?.let { id ->
                dictionaryApi.getDictionaryById(id)?.id?.toInt()?.let { dictionaryId: Int ->
                    termApi.getTermList(dictionaryId)
                        .map { term ->
                            TermUiItem(
                                id = term.word.id,
                                wordValue = term.word.value,
                                dictionaryId = term.word.dictionaryId,
                                addDate = term.word.addDate,
                                changeDate = term.word.changeDate,
                                lexemeList = term.lexemes
                                    .map { it.toDomain() }
                                    .map { it.toUiItem() }
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
                    lexemeList = term.lexemes
                        .map { it.toDomain() }
                        .map { it.toUiItem() }
                )
            }

        }
    }
}