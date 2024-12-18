package me.apomazkin.core_db_api.entity

import java.util.Date

@JvmInline
value class Source(val value: String)

data class SampleApiEntity(
    val id: Long,
    val lexemeId: Long? = null,
    val value: String,
    val source: Source?,
    val addDate: Date,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)