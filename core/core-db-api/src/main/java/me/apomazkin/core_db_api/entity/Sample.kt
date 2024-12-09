package me.apomazkin.core_db_api.entity

import java.util.Date

data class Sample(
    val id: Long,
    val lexemeId: Long? = null,
    val value: String,
    val source: String?,
    val addDate: Date,
    val changeDate: Date? = null,
)