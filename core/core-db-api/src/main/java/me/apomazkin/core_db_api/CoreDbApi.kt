package me.apomazkin.core_db_api

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.Dump
import me.apomazkin.core_db_api.entity.Hint
import me.apomazkin.core_db_api.entity.LanguageApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.SampleApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuiz

interface CoreDbApi {

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
        suspend fun searchTerms(pattern: String, langId: Long): List<TermApiEntity>
        suspend fun getTermById(id: Long): TermApiEntity?
    }

    interface LexemeApi {
        suspend fun getLexemeById(id: Long): LexemeApiEntity?
        suspend fun addLexeme(wordId: Long): Long
        suspend fun addLexeme(wordId: Long, translation: TranslationApiEntity): Long
        suspend fun addLexeme(wordId: Long, definition: DefinitionApiEntity): Long
        suspend fun updateLexemeTranslation(id: Long, translation: TranslationApiEntity?): Long?
        suspend fun updateLexemeDefinition(id: Long, definition: DefinitionApiEntity?): Long?
        suspend fun deleteLexeme(id: Long): Int
    }

    //New API
    
    fun addWordSuspend(value: String, langId: Int): Long
    suspend fun deleteWordSuspend(id: Long): Int
    suspend fun updateWordSuspend(id: Long, value: String): Boolean

    suspend fun addLexemeSuspend(wordId: Long, category: String, definition: String): Long
    suspend fun editLexemeSuspend(
        wordId: Long,
        lexemeId: Long,
        category: String,
        definition: String
    ): Int

    fun addWord(value: String, langId: Long): Completable
    fun getWord(id: Long): Single<WordApiEntity>
    fun getAllWord(): Single<List<WordApiEntity>>
    fun updateWord(wordApiEntity: WordApiEntity): Completable
    fun removeWord(id: Long): Completable

    fun wordCount(langId: Long): Single<Int>
    fun getDefinitionCount(): Single<Int>
    fun getDefinitionTypeCount(wordClass: String): Single<Int>
    fun getWriteQuizCountByGrade(tier: Int, langId: Long): Single<Int>


    fun getWriteQuizList(langId: Long): Single<List<WriteQuiz>>
    fun getWriteQuizList(limit: Int, langId: Long): Single<List<WriteQuiz>>
    fun getWriteQuizListByAccessTime(grade: Int, limit: Int, langId: Long): Single<List<WriteQuiz>>
    fun getRandomWriteQuizList(grade: Int, limit: Int, langId: Long): Single<List<WriteQuiz>>
    fun updateWriteQuizList(writeQuiz: WriteQuiz): Completable
    fun removeWriteQuiz(definitionId: Long): Completable

    fun addHint(definitionId: Long, value: String): Completable
    fun removeHint(id: Long): Completable
    fun removeHint(hint: Hint): Completable
    fun updateHint(hint: Hint): Completable

    fun addSample(definitionId: Long, value: String, source: String?): Completable
    fun getSampleList(definitionId: Long): Single<List<SampleApiEntity>>
    fun getSampleList(): Observable<List<SampleApiEntity>>

    fun getDump(): Single<Dump>
    fun restoreDump(dump: Dump)
}