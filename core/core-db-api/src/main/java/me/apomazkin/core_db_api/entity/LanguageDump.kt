package me.apomazkin.core_db_api.entity

import java.util.*

data class LanguageDump(
    val id: Long? = null,
    val numericCode: Int,
    val code: String,
    val name: String? = null,
    val addDate: Date,
    val changeDate: Date? = null,
) {
    companion object {
        const val RANGE = "Language!A1:E"
    }
}