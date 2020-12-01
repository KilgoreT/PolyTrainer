package me.apomazkin.core_db_api.entity

private const val NOUN_OFFSET = 0
private const val VERB_OFFSET = 5

private const val UNCOUNTABLE_INDEX = 0 + NOUN_OFFSET
private const val TRANSITIVE_INDEX = 0 + VERB_OFFSET

const val OPT_UNCOUNTABLE = 1L shl UNCOUNTABLE_INDEX
const val OPT_TRANSITIVE = 1L shl TRANSITIVE_INDEX

data class Definition(
    val id: Long? = null,
    val wordId: Long? = null,
    val definition: String? = null,
    val wordClass: WordClass? = null
)

sealed class WordClass(val options: Long = 0)
class Verb(options: Long = 0) : WordClass(options)
class Noun(options: Long = 0) : WordClass(options)
class Adjective(options: Long = 0) : WordClass(options)
class Adverb(options: Long = 0) : WordClass(options)