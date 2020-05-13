package me.apomazkin.core_db_api

interface CoreDbApi {
    fun insert(word: String)
    fun getList(): List<String>
}