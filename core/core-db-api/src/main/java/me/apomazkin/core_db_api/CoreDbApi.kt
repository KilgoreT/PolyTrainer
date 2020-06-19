package me.apomazkin.core_db_api

import io.reactivex.Observable
import me.apomazkin.core_db_api.entity.Word

interface CoreDbApi {
    fun insert(word: String)
    fun getWordList(): Observable<List<Word>>
}