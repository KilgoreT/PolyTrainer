package me.apomazkin.core_db_api.entity

import java.util.*

data class WriteQuizDump(
    val id: Long = 0,
    val langId: Long = 0,
    val definitionId: Long,
    val grade: Int = 0,
    val score: Int = 0,
    val addDate: Date? = null,
    val lastSelectDate: Date? = null,
) {
    companion object {
        const val RANGE = "Write!A1:G"
    }
}