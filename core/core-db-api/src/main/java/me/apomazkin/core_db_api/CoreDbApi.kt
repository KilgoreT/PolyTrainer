package me.apomazkin.core_db_api

import io.reactivex.Observable

interface CoreDbApi {
    fun insert(word: String)
    fun getList(): Observable<List<String>>
}