package me.apomazkin.core_db_api.entity

import java.util.Date

data class WordApiEntity(
    val id: Long = 0,
    val dictionaryId: Long,
    val value: String,
    val addDate: Date,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)