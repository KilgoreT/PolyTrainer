package me.apomazkin.wordcard.deps

import me.apomazkin.lexeme.Lexeme
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {
    suspend fun getTermById(wordId: Long): Term?
    suspend fun deleteWord(wordId: Long): Int
    suspend fun updateWord(wordId: Long, value: String): Boolean
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?
    suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?
    suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?
    suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?
    suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?

    /**
     * Восстановить лексему после full-delete (undo через snackbar). Создаёт новую
     * лексему c указанными translation/definition (хотя бы один обязателен). Возвращает
     * актуальный список лексем word'а или null при ошибке.
     */
    suspend fun restoreLexeme(
        wordId: Long,
        translation: String?,
        definition: String?,
    ): List<Lexeme>?
}

sealed interface RemoveTranslationResult {
    data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
    data object LexemeCascadeRemoved : RemoveTranslationResult
}

sealed interface RemoveDefinitionResult {
    data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
    data object LexemeCascadeRemoved : RemoveDefinitionResult
}
