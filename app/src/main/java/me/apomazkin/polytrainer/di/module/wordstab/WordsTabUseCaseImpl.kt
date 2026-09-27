package me.apomazkin.polytrainer.di.module.dictionarytab

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.entity.toUiItem
import me.apomazkin.flags.CountryProvider
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class WordsTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val wordApi: CoreDbApi.WordApi,
    private val termApi: CoreDbApi.TermApi,
    private val prefsProvider: PrefsProvider,
    private val countryProvider: CountryProvider,
    private val currentDictionaryProvider: CurrentDictionaryProvider,
) : WordsTabUseCase {

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

    // IS500: резолв текущего словаря — через единый CurrentDictionaryProvider.
    override fun flowCurrentDict(): Flow<DictUiEntity?> = currentDictionaryProvider
        .flowCurrentDict()
        .map { dict ->
            // IS476: null если ни по id, ни fallback ничего не нашли — валидное состояние
            dict?.let {
                DictUiEntity(
                    id = it.id,
                    flagRes = it.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                    title = it.name,
                    numericCode = it.numericCode ?: 0,
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