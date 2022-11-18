package me.apomazkin.core_db_api.entity

import java.util.*

data class SampleDump(
    val id: Long? = null,
    val definitionId: Long,
    val value: String,
    val source: String?,
    val addDate: Date,
    val changeDate: Date? = null,
) {
    companion object {
        const val RANGE = "Sample!A1:F"
    }
}