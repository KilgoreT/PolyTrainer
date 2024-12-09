package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "samples")
data class SampleDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val lexemeId: Long? = null,
    val value: String,
    val source: String?,
    val addDate: Date,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)