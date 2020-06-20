package me.apomazkin.core_db_api

import io.reactivex.Observable
import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_api.entity.WordWithDefinition

interface CoreDbApi {

    fun addWord(value: String)
    fun removeWord(id: Long)
    fun getWordList(): Observable<List<Word>>

    fun addDefinition(definition: Definition)
    fun getWordWithDefinition(): Observable<List<WordWithDefinition>>

}