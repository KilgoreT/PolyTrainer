package me.apomazkin.core_db_api.entity

import java.util.*

data class Word(
    val id: Long? = null,
    val langId: Long = 0,
    val value: String? = null,
    val addDate: Date? = null,
    val changeDate: Date? = null,
)