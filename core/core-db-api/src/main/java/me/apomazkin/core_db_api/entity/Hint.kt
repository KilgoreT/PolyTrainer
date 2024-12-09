package me.apomazkin.core_db_api.entity

import java.util.Date

data class Hint(
    val id: Long,
    val lexemeId: Long? = null,
    val value: String,
    val addDate: Date,
    val changeDate: Date? = null,
)