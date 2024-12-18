package me.apomazkin.wordcard.deps

import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(lexemeId: Long): Boolean
    suspend fun addLexeme(wordId: Long): Lexeme?
    suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long)
    suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long)
}