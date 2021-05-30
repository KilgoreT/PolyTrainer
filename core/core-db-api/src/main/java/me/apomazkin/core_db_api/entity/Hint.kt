package me.apomazkin.core_db_api.entity

import java.util.*

data class Hint(
    val id: Long,
    val definitionId: Long,
    val value: String,
    val addDate: Date,
    val changeDate: Date? = null,
)