package me.apomazkin.core_db_api

import io.reactivex.Observable
import me.apomazkin.core_db_api.entity.Word

interface CoreDbApi {
    fun insertWord(value: String)
    fun removeWord(id: Long)
    fun getWordList(): Observable<List<Word>>
}