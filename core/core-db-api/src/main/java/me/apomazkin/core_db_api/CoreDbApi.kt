package me.apomazkin.core_db_api

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_api.entity.*

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

    //New API

    suspend fun getLangSuspend(): List<Language>

    suspend fun getTermList(langId: Long): List<TermMate>
    suspend fun getTermById(id: Long): TermMate

    fun addWordSuspend(value: String, langId: Long): Long
    suspend fun deleteWordSuspend(id: Long): Int
    suspend fun updateWordSuspend(id: Long, value: String): Int

    suspend fun addLexemeSuspend(wordId: Long, category: String, definition: String): Long
    suspend fun editLexemeSuspend(
        wordId: Long,
        lexemeId: Long,
        category: String,
        definition: String
    ): Int

    suspend fun updateLexemeDefinition(definitionId: Long, value: String): Int
    suspend fun updateLexemeCategory(lexemeId: Long, category: String): Int

    suspend fun deleteLexemeSuspend(vararg id: Long): Int


    // Old API
    fun getLang(): Single<List<Language>>
    suspend fun addLangSuspend(numericCode: Int, name: String): Long
    fun getLangFlow(): Flow<List<Language>>

    fun addWord(value: String, langId: Long): Completable
    fun getWord(id: Long): Single<Word>
    fun getAllWord(): Single<List<Word>>
    fun updateWord(word: Word): Completable
    fun removeWord(id: Long): Completable

    fun addDefinition(definition: Definition, langId: Long): Completable
    fun getDefinitionAll(): Single<List<Definition>>
    fun getDefinition(id: Long): Single<Definition>
    fun getDefinitionListByWordId(wordId: Long): Single<List<Definition>>
    fun updateLexemeDefinition(definition: Definition): Completable
    fun removeDefinition(vararg id: Long): Completable

    fun getTermList(): Observable<List<Term>>
    fun searchTermList(pattern: String, langId: Long): Observable<List<Term>>

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
    fun getSampleList(definitionId: Long): Single<List<Sample>>
    fun getSampleList(): Observable<List<Sample>>

    fun getDump(): Single<Dump>
    fun restoreDump(dump: Dump)
}