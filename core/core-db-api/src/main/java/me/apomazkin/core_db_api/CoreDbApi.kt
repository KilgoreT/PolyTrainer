package me.apomazkin.core_db_api

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.LanguageApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity

interface CoreDbApi {
    
    interface DbInstance {
        suspend fun instance(): String
        suspend fun closeDatabase()
        suspend fun openDatabase()
        suspend fun isDatabaseOpen(): Boolean
        fun getDbInfo(): DbInfo
    }
    
    data class DbInfo(
        val mem: String,
        val name: String,
        val version: Int,
        val path: String,
        val isOpen: Boolean,
    )
    
    /**
     *
     * Word = sequence of letters.
     *
     * Lexeme = translation + definition + category + options
     *
     * LexicalCategory - word class, for example, noun, verb etc
     *
     * Term = Word + Lexeme(s)
     */
    
    /**
     * xxx -> xxxApiEntity -> xxxDb
     */
    
    interface LangApi {
        suspend fun addLang(numericCode: Int, name: String): Long
        suspend fun getLang(numericCode: Int): LanguageApiEntity?
        suspend fun getLangList(): List<LanguageApiEntity>
        fun flowLangList(): Flow<List<LanguageApiEntity>>
    }
    
    interface TermApi {
        suspend fun getTermList(langId: Int): List<TermApiEntity>
        suspend fun searchTerms(
            pattern: String,
            langId: Long
        ): List<TermApiEntity>

        fun searchTermsPaging(
                pattern: String,
                langId: Int
        ): Flow<PagingData<TermApiEntity>>
        
        suspend fun getTermById(id: Long): TermApiEntity?
    }
    
    interface LexemeApi {
        suspend fun getLexemeById(id: Long): LexemeApiEntity?
        suspend fun addLexeme(wordId: Long): Long
        suspend fun addLexeme(
            wordId: Long,
            translation: TranslationApiEntity
        ): Long
        
        suspend fun addLexeme(
            wordId: Long,
            definition: DefinitionApiEntity
        ): Long
        
        suspend fun updateLexemeTranslation(
            id: Long,
            translation: TranslationApiEntity?
        ): Long?
        
        suspend fun updateLexemeDefinition(
            id: Long,
            definition: DefinitionApiEntity?
        ): Long?
        
        suspend fun deleteLexeme(id: Long): Int
    }
    
    interface QuizApi {
        suspend fun addWriteQuiz(langId: Long, lexemeId: Long): Long
        suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertApiEntity>): Int
        
        suspend fun getRandomWriteQuizList(
            grade: Int,
            limit: Int,
            langId: Long
        ): List<WriteQuizComplexEntity>
    }
    
    //New API
    fun addWordSuspend(value: String, langId: Int): Long
    suspend fun deleteWordSuspend(id: Long): Int
    suspend fun updateWordSuspend(id: Long, value: String): Boolean
    
    suspend fun addLexemeSuspend(
        wordId: Long,
        category: String,
        definition: String
    ): Long
    
    suspend fun editLexemeSuspend(
        wordId: Long,
        lexemeId: Long,
        category: String,
        definition: String
    ): Int
    
}