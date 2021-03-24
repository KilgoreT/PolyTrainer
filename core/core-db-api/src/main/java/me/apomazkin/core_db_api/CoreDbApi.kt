package me.apomazkin.core_db_api

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_api.entity.WordWithDefinition
import me.apomazkin.core_db_api.entity.WriteQuiz

interface CoreDbApi {

    fun addWord(value: String)
    fun getWord(id: Long): Single<Word>
    fun removeWord(id: Long)

    fun addDefinition(definition: Definition): Completable
    fun getDefinition(id: Long): Single<Definition>
    fun deleteDefinition(id: Long): Completable
    fun getWordWithDefinition(): Observable<List<WordWithDefinition>>

    fun deleteWord(id: Long): Completable

    fun wordCount(): Single<Int>
    fun getDefinitionCount(): Single<Int>
    fun getDefinitionTypeCount(wordClass: String): Single<Int>

    fun getWriteQuizList(grade: Int, limit: Int): Single<List<WriteQuiz>>
    fun updateWriteQuizList(writeQuiz: WriteQuiz): Completable

}