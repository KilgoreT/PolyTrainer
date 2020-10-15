package me.apomazkin.core_db_api.entity

sealed class PartOfSpeech
data class Verb(val isTransitive: Boolean?) : PartOfSpeech()
data class Noun(val isCountable: Boolean?) : PartOfSpeech()
object Adjective : PartOfSpeech()
object Adverb : PartOfSpeech()