package me.apomazkin.core_db_api.entity

import java.util.Date

data class DictionaryApiEntity(
    val id: Long,
    val numericCode: Int?,
    val name: String,
    val addDate: Date,
    val changeDate: Date? = null,
    val deleteDate: Date? = null,
)
