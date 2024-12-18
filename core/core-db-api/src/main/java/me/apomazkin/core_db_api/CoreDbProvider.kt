package me.apomazkin.core_db_api

interface CoreDbProvider {
    fun getCoreDbApi(): CoreDbApi
    fun getTermApi(): CoreDbApi.TermApi
    fun getLexemeApi(): CoreDbApi.LexemeApi
}