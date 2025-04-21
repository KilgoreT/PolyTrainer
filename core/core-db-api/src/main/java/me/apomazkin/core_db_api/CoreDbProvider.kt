package me.apomazkin.core_db_api

interface CoreDbProvider {
    fun getCoreDbApi(): CoreDbApi
    fun getDbInstance(): CoreDbApi.DbInstance
    fun getLangApi(): CoreDbApi.LangApi
    fun getTermApi(): CoreDbApi.TermApi
    fun getLexemeApi(): CoreDbApi.LexemeApi
    fun getQuizApi(): CoreDbApi.QuizApi
}