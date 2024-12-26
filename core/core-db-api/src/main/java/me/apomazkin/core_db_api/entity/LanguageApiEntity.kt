package me.apomazkin.core_db_api.entity

import java.util.Date

data class LanguageApiEntity(
    val id: Int,
    val numericCode: Int,
    val code: String,
    val name: String,
    val addDate: Date,
    val changeDate: Date? = null,
    val deleteDate: Date? = null,
)