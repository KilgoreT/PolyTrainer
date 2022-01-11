package me.apomazkin.core_db_api

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.entity.*

interface CoreDbApi {

    fun addWord(value: String, langId: Long): Completable
    fun getWord(id: Long): Single<Word>
    fun updateWord(word: Word): Completable
    fun removeWord(id: Long): Completable

    fun addDefinition(definition: Definition, langId: Long): Completable
    fun getDefinitionAll(): Single<List<Definition>>
    fun getDefinition(id: Long): Single<Definition>
    fun getDefinitionListByWordId(wordId: Long): Single<List<Definition>>
    fun updateDefinition(definition: Definition): Completable
    fun removeDefinition(vararg id: Long): Completable

    fun getTermList(): Observable<List<Term>>
    fun searchTermList(pattern: String, langId: Long): Observable<List<Term>>

    fun wordCount(): Single<Int>
    fun getDefinitionCount(): Single<Int>
    fun getDefinitionTypeCount(wordClass: String): Single<Int>
    fun getWriteQuizCountByGrade(tier: Int): Single<Int>


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
}