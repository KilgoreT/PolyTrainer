package me.apomazkin.core_db_api

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Quiz
import me.apomazkin.core_db_api.entity.WordWithDefinition

interface CoreDbApi {

    fun addWord(value: String)
    fun removeWord(id: Long)

    fun addDefinition(definition: Definition)
    fun deleteDefinition(id: Long): Completable
    fun getWordWithDefinition(): Observable<List<WordWithDefinition>>

    fun deleteWord(id: Long): Completable

    fun wordCount(): Single<Int>
    fun getDefinitionCount(): Single<Int>
    fun getDefinitionTypeCount(wordClass: String): Single<Int>

    fun getRandomQuizList(): Single<List<Quiz>>

}