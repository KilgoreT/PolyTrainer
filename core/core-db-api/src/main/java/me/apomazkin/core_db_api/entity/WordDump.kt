package me.apomazkin.core_db_api.entity

import java.util.*

data class WordDump(
    val id: Long? = null,
    val langId: Long = 0,
    val word: String? = null,
    val addDate: Date? = null,
    val changeDate: Date? = null,
) {
    companion object {
        const val RANGE = "Word!A1:E"
    }
}