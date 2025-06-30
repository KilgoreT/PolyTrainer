package me.apomazkin.dictionarytab.deps

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.TermUiItem

interface DictionaryTabUseCase {
    suspend fun getLangId(numericCode: Int): Int

    //TODO kilg 29.06.2025 21:33 завести слой доменных сущностей.
    suspend fun getCurrentDict(): DictUiEntity
    fun flowCurrentDict(): Flow<DictUiEntity>
    suspend fun changeDict(numericCode: Int)

    suspend fun getWordList(): List<TermUiItem>
    fun searchTerms(
        pattern: String,
        langId: Int
    ): Flow<PagingData<TermUiItem>>

    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean
    suspend fun addLexeme(wordId: Long, category: String, definition: String)
    suspend fun editLexeme(wordId: Long, lexemeId: Long, category: String, definition: String)
    suspend fun deleteLexeme(lexemeId: Long): Boolean
}

class LangNotFoundException: IllegalStateException("No Languages found")