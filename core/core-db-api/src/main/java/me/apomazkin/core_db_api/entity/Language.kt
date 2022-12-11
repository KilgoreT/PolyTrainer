package me.apomazkin.core_db_api.entity

import java.util.*

data class Language(
    val id: Long? = null,
    val code: String,
    val name: String? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)