package me.apomazkin.core_db_api.entity

/**
 * @param partOfSpeech - какой тип? глагол или существительное?
 */
data class Definition(
    val id: Long? = null,
    val wordId: Long? = null,
    val definition: String? = null,
    val partOfSpeech: PartOfSpeech? = null
)