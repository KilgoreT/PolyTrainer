package me.apomazkin.core_db_api

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Term
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_api.entity.WriteQuiz

interface CoreDbApi {

    fun addWord(value: String): Completable
    fun getWord(id: Long): Single<Word>
    fun updateWord(word: Word): Completable
    fun removeWord(id: Long): Completable

    fun addDefinition(definition: Definition): Completable
    fun getDefinition(id: Long): Single<Definition>
    fun getDefinitionListByWordId(wordId: Long): Single<List<Definition>>
    fun updateDefinition(definition: Definition): Completable
    fun removeDefinition(id: Long): Completable

    fun getTermList(): Observable<List<Term>>
    fun searchTermList(pattern: String): Observable<List<Term>>

    fun wordCount(): Single<Int>
    fun getDefinitionCount(): Single<Int>
    fun getDefinitionTypeCount(wordClass: String): Single<Int>
    fun getWriteQuizCountByGrade(tier: Int): Single<Int>


    fun getWriteQuizList(grade: Int, limit: Int): Single<List<WriteQuiz>>
    fun updateWriteQuizList(writeQuiz: WriteQuiz): Completable
    fun removeWriteQuiz(definitionId: Long): Completable

}