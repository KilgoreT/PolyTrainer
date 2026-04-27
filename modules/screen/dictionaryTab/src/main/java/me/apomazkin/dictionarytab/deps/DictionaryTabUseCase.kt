package me.apomazkin.dictionarytab.deps

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.TermUiItem

interface DictionaryTabUseCase {
    //TODO kilg 29.06.2025 21:33 завести слой доменных сущностей.
    suspend fun getCurrentDict(): DictUiEntity
    fun flowCurrentDict(): Flow<DictUiEntity>
    suspend fun changeDict(id: Long)

    suspend fun getWordList(): List<TermUiItem>
    fun searchTerms(
        pattern: String,
        dictionaryId: Int
    ): Flow<PagingData<TermUiItem>>

    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean
}

class DictionaryNotFoundException: IllegalStateException("No Dictionaries found")