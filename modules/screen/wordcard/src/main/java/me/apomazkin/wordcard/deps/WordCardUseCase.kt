package me.apomazkin.wordcard.deps

import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(lexemeId: Long): Int
    suspend fun addLexeme(wordId: Long, category: String, definition: String): Long
    suspend fun updateLexicalDefinition(lexemeId: Long, value: String): Int
    suspend fun updateLexicalCategory(lexemeId: Long, category: String): Int
}