package me.apomazkin.vocabulary.deps

import me.apomazkin.vocabulary.entity.LangUiEntity
import me.apomazkin.vocabulary.entity.TermUiItem

interface VocabularyUseCase {
    suspend fun getCurrentLang(): Int
    suspend fun getAvailableLang(): List<LangUiEntity>
    suspend fun changeLang(numericCode: Int)
    suspend fun getWordList(): List<TermUiItem>
    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun addLexeme(wordId: Long, category: String, definition: String)
    suspend fun editLexeme(wordId: Long, lexemeId: Long, category: String, definition: String)
    suspend fun deleteLexeme(lexemeId: Long)
}