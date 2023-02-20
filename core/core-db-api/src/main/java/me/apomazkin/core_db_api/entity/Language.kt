package me.apomazkin.core_db_api.entity

import java.util.*

data class Language(
    val id: Long? = null,
    val numericCode: Int,
    val code: String,
    // TODO: сделать ненулабельным
    val name: String? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)