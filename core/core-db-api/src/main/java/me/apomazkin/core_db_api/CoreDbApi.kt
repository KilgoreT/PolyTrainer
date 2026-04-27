package me.apomazkin.core_db_api

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
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

    interface DictionaryApi {
        suspend fun addDictionary(name: String, numericCode: Int? = null): Long
        suspend fun getDictionary(numericCode: Int): DictionaryApiEntity?
        suspend fun getDictionaryById(id: Long): DictionaryApiEntity?
        suspend fun getDictionaryList(): List<DictionaryApiEntity>
        suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
        suspend fun deleteDictionary(id: Long)
        fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>
    }

    interface TermApi {
        suspend fun getTermList(dictionaryId: Int): List<TermApiEntity>
        suspend fun searchTerms(
            pattern: String,
            dictionaryId: Long,
        ): List<TermApiEntity>

        fun searchTermsPaging(
            pattern: String,
            dictionaryId: Int,
        ): Flow<PagingData<TermApiEntity>>

        suspend fun getTermById(id: Long): TermApiEntity?
    }

    interface WordApi {
        fun addWordSuspend(value: String, dictionaryId: Int): Long
        suspend fun deleteWordSuspend(id: Long): Int
        suspend fun updateWordSuspend(id: Long, value: String): Boolean
    }

    interface LexemeApi {
        suspend fun getLexemeById(id: Long): LexemeApiEntity?
        suspend fun addLexeme(wordId: Long): Long
        suspend fun addLexeme(
            wordId: Long,
            translation: TranslationApiEntity,
        ): Long

        suspend fun addLexeme(
            wordId: Long,
            definition: DefinitionApiEntity,
        ): Long

        suspend fun updateLexemeTranslation(
            id: Long,
            translation: TranslationApiEntity?,
        ): Long?

        suspend fun updateLexemeDefinition(
            id: Long,
            definition: DefinitionApiEntity?,
        ): Long?

        suspend fun deleteLexeme(id: Long): Int
    }

    interface QuizApi {
        suspend fun addWriteQuiz(dictionaryId: Long, lexemeId: Long): Long
        suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertApiEntity>): Int

        suspend fun getRandomWriteQuizList(
            grade: Int,
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity>

        suspend fun getEarliestWriteQuizList(
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity>

        suspend fun getFrequentMistakesWriteQuizList(
            limit: Int,
            dictionaryId: Long,
        ): List<WriteQuizComplexEntity>
    }

    interface StatisticApi {
        fun flowWordCount(dictionaryId: Int): Flow<Int>
        fun flowLexemeCount(dictionaryId: Int): Flow<Int>
        fun flowQuizCount(dictionaryId: Int, maxGrade: Int): Flow<Map<Int, Int>>
    }
}
