package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo

/** IS532: проекция `id, value` слова — тексты словаря без лексем (AWP-07). */
data class WordTextDb(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "value") val value: String,
)
