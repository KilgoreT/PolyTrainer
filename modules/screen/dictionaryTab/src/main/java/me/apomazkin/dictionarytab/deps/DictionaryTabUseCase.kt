package me.apomazkin.dictionarytab.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.TermUiItem

interface DictionaryTabUseCase {
    suspend fun getCurrentDict(): DictUiEntity
    suspend fun getAvailableDict(): List<DictUiEntity>
    fun flowAvailableDict(): Flow<List<DictUiEntity>>
    suspend fun changeDict(numericCode: Int)
    suspend fun getWordList(): List<TermUiItem>
    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean
    suspend fun addLexeme(wordId: Long, category: String, definition: String)
    suspend fun editLexeme(wordId: Long, lexemeId: Long, category: String, definition: String)
    suspend fun deleteLexeme(lexemeId: Long): Boolean
}