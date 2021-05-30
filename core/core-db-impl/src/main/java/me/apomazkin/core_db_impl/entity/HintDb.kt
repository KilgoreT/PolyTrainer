package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "hint")
data class HintDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val definitionId: Long,
    val value: String,
    val addDate: Date,
    val changeDate: Date? = null,
)