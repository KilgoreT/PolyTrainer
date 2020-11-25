package me.apomazkin.core_db_api.entity

/**
 * @param wordClass - какой тип? глагол или существительное?
 */
data class Definition(
    val id: Long? = null,
    val wordId: Long? = null,
    val definition: String? = null,
    val wordClass: WordClass? = null
)