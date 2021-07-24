package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "sample")
data class SampleDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val definitionId: Long,
    val value: String,
    val source: String?,
    val addDate: Date,
    val changeDate: Date? = null,
)