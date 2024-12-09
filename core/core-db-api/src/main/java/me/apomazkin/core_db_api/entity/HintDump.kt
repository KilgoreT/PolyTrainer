package me.apomazkin.core_db_api.entity

import java.util.Date

data class HintDump(
    val id: Long? = null,
    val lexemeId: Long? = null,
    val value: String,
    val addDate: Date,
    val changeDate: Date? = null,
) {
    companion object {
        const val RANGE = "Hint!A1:E"
    }
}