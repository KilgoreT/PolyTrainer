package me.apomazkin.core_db_api.entity

sealed class WordClass
data class Verb(val isTransitive: Boolean?) : WordClass()
data class Noun(val isCountable: Boolean?) : WordClass()
object Adjective : WordClass()
object Adverb : WordClass()