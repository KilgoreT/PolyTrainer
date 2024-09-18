package me.apomazkin.vocabulary.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.vocabulary.entity.DictUiEntity
import me.apomazkin.vocabulary.entity.TermUiItem

interface VocabularyUseCase {
    suspend fun getCurrentDict(): Int
    suspend fun getAvailableDict(): List<DictUiEntity>
    fun flowAvailableDict(): Flow<List<DictUiEntity>>
    suspend fun changeDict(numericCode: Int)
    suspend fun getWordList(): List<TermUiItem>
    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun addLexeme(wordId: Long, category: String, definition: String)
    suspend fun editLexeme(wordId: Long, lexemeId: Long, category: String, definition: String)
    suspend fun deleteLexeme(lexemeId: Long)
}