package me.apomazkin.core_db_api.entity

/** IS532: слово словаря без лексем — для сравнения текстов без регистра (AWP-07). */
data class WordTextApiEntity(
    val id: Long,
    val value: String,
)
